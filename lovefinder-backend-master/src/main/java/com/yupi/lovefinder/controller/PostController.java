package com.yupi.lovefinder.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.yupi.lovefinder.annotation.AuthCheck;
import com.yupi.lovefinder.common.BaseResponse;
import com.yupi.lovefinder.common.DeleteRequest;
import com.yupi.lovefinder.common.ErrorCode;
import com.yupi.lovefinder.common.ResultUtils;
import com.yupi.lovefinder.constant.CommonConstant;
import com.yupi.lovefinder.exception.BusinessException;
import com.yupi.lovefinder.model.dto.PostAddRequest;
import com.yupi.lovefinder.model.dto.PostQueryRequest;
import com.yupi.lovefinder.model.dto.PostDoThumbRequest;
import com.yupi.lovefinder.model.dto.PostUpdateRequest;
import com.yupi.lovefinder.model.entity.Post;
import com.yupi.lovefinder.model.entity.PostThumb;
import com.yupi.lovefinder.model.entity.User;
import com.yupi.lovefinder.model.vo.PostVO;
import com.yupi.lovefinder.service.PostService;
import com.yupi.lovefinder.service.PostThumbService;
import com.yupi.lovefinder.service.UserService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子接口
 *
 * @author yupi
 */
@RestController
@RequestMapping("/post")
@Slf4j
public class PostController {

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    @Resource
    private PostThumbService postThumbService;

    // IO 型线程池
    /**
     * corePoolSize:指定了线程池中的线程数量，它的数量决定了添加的任务是开辟新的线程去执行，还是放到workQueue任务队列中去；
     maximumPoolSize:指定了线程池中的最大线程数量，这个参数会根据你使用的workQueue任务队列的类型，决定线程池会开辟的最大线程数量；
     keepAliveTime:当线程池中空闲线程数量超过corePoolSize时，多余的线程会在多长时间内被销毁
     unit:keepAliveTime的单位
     workQueue:任务队列，被添加到线程池中，但尚未被执行的任务；它一般分为直接提交队列、有界任务队列、无界任务队列、优先任务队列几种；
     使用ArrayBlockingQueue有界任务队列，若有新的任务需要执行时，线程池会创建新的线程，
     直到创建的线程数量达到corePoolSize时，则会将新的任务加入到等待队列中。
     若等待队列已满，即超过ArrayBlockingQueue初始化的容量，则继续创建线程，
     直到线程数量达到maximumPoolSize设置的最大线程数量，若大于maximumPoolSize，
     则执行拒绝策略。在这种情况下，线程数量的上限与有界任务队列的状态有直接关系，
     如果有界队列初始容量较大或者没有达到超负荷的状态，线程数将一直维持在corePoolSize以下，
      反之当任务队列已满时，则会以maximumPoolSize为最大线程数上限。
     */
    private final ExecutorService ioExecutorService = new ThreadPoolExecutor(4, 20, 10, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000));

    // 单帖子获取缓存，key 为 postId，value 为 post
    //自动加载缓存
    LoadingCache<Long, Post> postGetCache = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(5_000).build(postId -> postService.getById(postId));

    // region 增删改查

    /**
     * 创建
     *
     * @param postAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addPost(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request) {
        if (postAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Post post = new Post();
        BeanUtils.copyProperties(postAddRequest, post);
        // 校验
        postService.validPost(post, true);
        User loginUser = userService.getLoginUser(request);
        post.setUserId(loginUser.getId());
        boolean result = postService.save(post);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 移除缓存
        long newPostId = post.getId();
        postGetCache.invalidate(newPostId);
        return ResultUtils.success(newPostId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePost(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Post oldPost = postService.getById(id);
        if (oldPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldPost.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = postService.removeById(id);
        // 异步删除点赞信息
        CompletableFuture.runAsync(() -> {
            QueryWrapper<PostThumb> queryWrapper = new QueryWrapper<>();
            long postId = oldPost.getId();
            queryWrapper.eq("postId", postId);
            boolean result = postThumbService.remove(queryWrapper);
            if (!result) {
                log.error("postThumb delete failed, postId = {}", postId);
            }
        }, ioExecutorService);
        // 移除缓存
        postGetCache.invalidate(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param postUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePost(@RequestBody PostUpdateRequest postUpdateRequest,
                                            HttpServletRequest request) {
        if (postUpdateRequest == null || postUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Post post = new Post();
        BeanUtils.copyProperties(postUpdateRequest, post);
        // 参数校验
        postService.validPost(post, false);
        User user = userService.getLoginUser(request);
        long id = postUpdateRequest.getId();
        // 判断是否存在
        Post oldPost = postService.getById(id);
        if (oldPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldPost.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = postService.updateById(post);
        // 移除缓存
        postGetCache.invalidate(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Post> getPostById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Post post = postGetCache.get(id);
        return ResultUtils.success(post);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param postQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<Post>> listPost(PostQueryRequest postQueryRequest) {
        Post postQuery = new Post();
        if (postQueryRequest != null) {
            BeanUtils.copyProperties(postQueryRequest, postQuery);
        }
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>(postQuery);
        List<Post> postList = postService.list(queryWrapper);
        return ResultUtils.success(postList);
    }

    /**
     * 分页获取列表
     *
     * @param postQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<PostVO>> listPostByPage(PostQueryRequest postQueryRequest, HttpServletRequest request) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Post postQuery = new Post();
        BeanUtils.copyProperties(postQueryRequest, postQuery);
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        String content = postQuery.getContent();
        // content 需支持模糊搜索
        postQuery.setContent(null);
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>(postQuery);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        //分页当前页数
        Page<Post> postPage = postService.page(new Page<>(current, size), queryWrapper);

        Map<Long, List<PostVO>> postIdListMap = postPage.getRecords().stream().map(post -> {
            PostVO postVO = new PostVO();
            BeanUtils.copyProperties(post, postVO);
            postVO.setHasThumb(false);
            return postVO;
        }).collect(Collectors.groupingBy(Post::getId));
        // 已登录，获取用户点赞状态
        try {
            //SELECT id,postId,userId,createTime,updateTime FROM post_thumb WHERE (postId IN (?,?,?,?,?,?,?,?,?,?) AND userId = ?)
            User user = userService.getLoginUser(request);
            QueryWrapper<PostThumb> postThumbQueryWrapper = new QueryWrapper<>();
            postThumbQueryWrapper.in("postId", postIdListMap.keySet());
            postThumbQueryWrapper.eq("userId", user.getId());
            List<PostThumb> postThumbList = postThumbService.list(postThumbQueryWrapper);
            postThumbList.forEach(postThumb ->
                    postIdListMap.get(postThumb.getPostId()).get(0).setHasThumb(true));
        } catch (Exception e) {
            // 用户未登录，不处理

        }
        Page<PostVO> postVOPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        postVOPage.setRecords(postIdListMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
        return ResultUtils.success(postVOPage);
    }

    // endregion

    /**
     * 点赞 / 取消点赞
     *
     * @param postDoThumbRequest
     * @param request
     * @return resultNum 本次点赞变化数
     */
    @PostMapping("/thumb")
    public BaseResponse<Integer> postDoThumb(@RequestBody PostDoThumbRequest postDoThumbRequest,
                                         HttpServletRequest request) {
        if (postDoThumbRequest == null || postDoThumbRequest.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 必须登录
        final User loginUser = userService.getLoginUser(request);
        long postId = postDoThumbRequest.getPostId();
        int result = postThumbService.doThumb(postId, loginUser);
        if (result != 0) {
            // 移除缓存
            postGetCache.invalidate(postId);
        }
        return ResultUtils.success(result);
    }

}
