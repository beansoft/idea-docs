---
title: 汉化说明
redirect_from:
- /index.html
---

本文档基于 [https://github.com/kana112233/intellij-sdk-docs-cn](https://github.com/kana112233/intellij-sdk-docs-cn) 的
热心工作并有修改, 原始文档和本项目文档均遵循Apache开源协议. 需要说明的是JetBrains的最新插件开发文档已经不支持对外提供编译支持, 无法本地编译运行预览.

本文档并非最新文档, 仅为了帮助了解插件开发而翻译, 本人不对内容有效性和及时性提供任何担保, 也不为因文档中出现的任何翻译错误承担责任, 否则请勿阅读使用本文档. 最新文档有效性以官方英文文档为准: 
[https://plugins.jetbrains.com/docs/intellij/welcome.html](https://plugins.jetbrains.com/docs/intellij/welcome.html).

付费插件开发文档请阅读官方英文文档, 暂时未有计划翻译. [https://plugins.jetbrains.com/docs/marketplace/](https://plugins.jetbrains.com/docs/marketplace/)

Markdown 文档必须符合YAMl格式,否则会报错, 请阅读: [SDK文档的书写风格](intro/sdk_style.md)

[导航边栏请修改此文件](_SUMMARY.md) _SUMMARY.md

BeanSoft@126.com, 网名 BeanSoft, 资深架构师, 曾就职于甲骨文北京, 易到用车等公司, 业余热爱IDEA插件开发与汉化, 荣获 [JetBrains Community Contributor](https://www.jetbrains.com/zh-cn/lp/jetbrains-community-contributor/) 社区贡献者称号.

BeanSoft开发的插件: [https://plugins.jetbrains.com/organizations/BeanSoft](https://plugins.jetbrains.com/organizations/BeanSoft)

B站视频: [IDEA插件社区](https://space.bilibili.com/297314170) 

QQ技术交流群：209711104

部署编译
rake build[_config.yml]

[![官方JetBrains项目](https://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![Twitter Follow](https://img.shields.io/twitter/follow/JBPlatform?style=flat-square&logo=twitter)](https://twitter.com/JBPlatform/) [![加入聊天 Slack](https://img.shields.io/badge/Slack-%23intellij--platform-blue?style=flat-square&logo=slack)](https://plugins.jetbrains.com/slack)


IntelliJ Platform SDK Documentation
=======

Welcome to the repository for [IntelliJ Platform SDK Documentation](http://www.jetbrains.org/intellij/sdk/docs/) site.

## Reporting Bugs
Please report any content inconsistencies, outdated materials, cosmetic issues and other defects you find in the docs and samples by submitting an issue to
[YouTrack](https://youtrack.jetbrains.com/issues/IJSDK). 

## Working With the Site Locally
To check out and run a local copy of the site follow the steps described below.

### Pre-requirements

*  Make sure you have a 
   [git client](https://git-scm.com/downloads)
   installed

*  This site requires
   [Ruby 2.0](https://www.ruby-lang.org/) or higher.
   Follow the official Ruby language
   [download](https://www.ruby-lang.org/en/downloads/)
   and
   [installation](https://www.ruby-lang.org/en/documentation/installation/)
   instructions to get Ruby working on your machine.
   
*  This site requires [Jekyll](https://jekyllrb.com/), 
   a Ruby-based site generating framework.
   To install Jekyll refer to its
   [installation guidelines](https://jekyllrb.com/docs/installation/).
   **Note:** If you are using Windows, you can face some specific aspects while installing Jekyll.
   See this [Run Jekyll on Windows Guide](https://jekyll-windows.juthilo.com/) to get more information.
   
### Checking Out Site Repository

To check out the source code run the following command:

```bash
git clone https://github.com/JetBrains/intellij-sdk-docs.git
```
   
### Initializing Submodules

The site uses JetBrains custom web templates.
To enable custom templates locally, you need to initialize repository submodules.
Run the following command in the checkout directory to do so.
 
```bash
git submodule update --init --remote
```

### Installing Gems

After you performed the initial checkout for the main repository and the submodule, run `bundle install` to install additionally required gems.

### Building and Previewing 
A set of Rake tasks, a Make-like program implemented in Ruby, provides short commands to build and run the site locally.

#### Building Site from Sources
 
*  Make sure you are in a project root directory
*  To build static site content run
   ```
   rake build
   ```
   
#### Previewing

*  To start the web-server run
    ```
    rake preview
    ```
*  Open the address
   [http://localhost:4000/intellij/sdk/docs/](http://localhost:4000/intellij/sdk/docs/)
   in your browser.
   **Note:** Make sure you haven't changed default Jekyll port during installation.


