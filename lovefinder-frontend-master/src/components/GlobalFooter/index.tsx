import { CrownOutlined, GithubOutlined, UserOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';
import './index.less';

/**
 * 全局 Footer
 *
 * @author yupi
 */
const GlobalFooter: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <DefaultFooter
      className="default-footer"
      copyright={`${currentYear} 程序员鱼皮 + 备案号`}
      links={[
        {
          key: '1',
          title: (
            <>
              <UserOutlined /> 站长：程序员鱼皮
            </>
          ),
          href: 'https://space.bilibili.com/12890453/',
          blankTarget: true,
        },
        {
          key: '2',
          title: (
            <>
              <GithubOutlined /> 开源地址
            </>
          ),
          href: 'https://github.com/liyupi/',
          blankTarget: true,
        },
        {
          key: '3',
          title: (
            <>
              <CrownOutlined /> 编程导航知识星球
            </>
          ),
          href: 'https://yupi.icu/',
          blankTarget: true,
        },
      ]}
    />
  );
};

export default GlobalFooter;
