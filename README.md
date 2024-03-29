#1.简介

## 1.1.什么是 oAuth

oAuth 协议为用户资源的授权提供了一个安全的、开放而又简易的标准。与以往的授权方式不同之处是 oAuth 的授权不会使第三方触及到用户的帐号信息（如用户名与密码），即第三方无需使用用户的用户名与密码就可以申请获得该用户资源的授权，因此 oAuth 是安全的。

## 1.2.什么是 Spring Security

Spring Security 是一个安全框架，前身是 Acegi Security，能够为 Spring 企业应用系统提供声明式的安全访问控制。Spring Security 基于 Servlet 过滤器、IoC 和 AOP，为 Web 请求和方法调用提供身份确认和授权处理，避免了代码耦合，减少了大量重复代码工作。

## 1.3.为什么需要 oAuth2

### 1.3.1.应用场景

我们假设你有一个“云笔记”产品，并提供了“云笔记服务”和“云相册服务”，此时用户需要在不同的设备（PC、Android、iPhone、TV、Watch）上去访问这些“资源”（笔记，图片）

那么用户如何才能访问属于自己的那部分资源呢？此时传统的做法就是提供自己的账号和密码给我们的“云笔记”，登录成功后就可以获取资源了。但这样的做法会有以下几个问题：

- “云笔记服务”和“云相册服务”会分别部署，难道我们要分别登录吗？
- 如果有第三方应用程序想要接入我们的“云笔记”，难道需要用户提供账号和密码给第三方应用程序，让他记录后再访问我们的资源吗？
- 用户如何限制第三方应用程序在我们“云笔记”的授权范围和使用期限？难道把所有资料都永久暴露给它吗？
- 如果用户修改了密码收回了权限，那么所有第三方应用程序会全部失效。
- 只要有一个接入的第三方应用程序遭到破解，那么用户的密码就会泄露，后果不堪设想。

为了解决如上问题，oAuth 应用而生。

### 1.3.2.名词解释

- **第三方应用程序（Third-party application）：** 又称之为客户端（client），比如上节中提到的设备（PC、Android、iPhone、TV、Watch），我们会在这些设备中安装我们自己研发的 APP。又比如我们的产品想要使用 QQ、微信等第三方登录。对我们的产品来说，QQ、微信登录是第三方登录系统。我们又需要第三方登录系统的资源（头像、昵称等）。对于 QQ、微信等系统我们又是第三方应用程序。
- **HTTP 服务提供商（HTTP service）：** 我们的云笔记产品以及 QQ、微信等都可以称之为“服务提供商”。
- **资源所有者（Resource Owner）：** 又称之为用户（user）。
- **用户代理（User Agent）：** 比如浏览器，代替用户去访问这些资源。
- **认证服务器（Authorization server）：** 即服务提供商专门用来处理认证的服务器，简单点说就是登录功能（验证用户的账号密码是否正确以及分配相应的权限）
- **资源服务器（Resource server）：** 即服务提供商存放用户生成的资源的服务器。它与认证服务器，可以是同一台服务器，也可以是不同的服务器。简单点说就是资源的访问入口，比如上节中提到的“云笔记服务”和“云相册服务”都可以称之为资源服务器。

### 1.3.3.交互过程

oAuth 在 "客户端" 与 "服务提供商" 之间，设置了一个授权层（authorization layer）。"客户端" 不能直接登录 "服务提供商"，只能登录授权层，以此将用户与客户端区分开来。"客户端" 登录授权层所用的令牌（token），与用户的密码不同。用户可以在登录的时候，指定授权层令牌的权限范围和有效期。"客户端" 登录授权层以后，"服务提供商" 根据令牌的权限范围和有效期，向 "客户端" 开放用户储存的资料。

![](images/oAuth2流程.png)

# 2.开放平台

## 2.1.交互模型

交互模型涉及三方：

- 资源拥有者：用户
- 客户端：APP
- 服务提供方：包含两个角色
  - 认证服务器
  - 资源服务器

## 2.2.认证服务器

认证服务器负责对用户进行认证，并授权给客户端权限。认证很容易实现（验证账号密码即可），问题在于如何授权。比如我们使用第三方登录 "有道云笔记"，你可以看到如使用 QQ 登录的授权页面上有 "有道云笔记将获得以下权限" 的字样以及权限信息

![img](images/认证案例.png)

认证服务器需要知道请求授权的客户端的身份以及该客户端请求的权限。我们可以为每一个客户端预先分配一个 id，并给每个 id 对应一个名称以及权限信息。这些信息可以写在认证服务器上的配置文件里。然后，客户端每次打开授权页面的时候，把属于自己的 id 传过来，如：

```
http://www.funtl.com/login?client_id=itsClientId
```

随着时间的推移和业务的增长，会发现，修改配置的工作消耗了太多的人力。有没有办法把这个过程自动化起来，把人工从这些繁琐的操作中解放出来？当开始考虑这一步，开放平台的成型也就是水到渠成的事情了。

## 2.3.oAuth2 开放平台

开放平台是由 oAuth2.0 协议衍生出来的一个产品。它的作用是让客户端自己去这上面进行注册、申请，通过之后系统自动分配 `client_id` ，并完成配置的自动更新（通常是写进数据库）。

客户端要完成申请，通常需要填写客户端程序的类型（Web、App 等）、企业介绍、执照、想要获取的权限等等信息。这些信息在得到服务提供方的人工审核通过后，开发平台就会自动分配一个 `client_id` 给客户端了。

到这里，已经实现了登录认证、授权页的信息展示。那么接下来，当用户成功进行授权之后，认证服务器需要把产生的 `access_token` 发送给客户端，方案如下：

- 让客户端在开放平台申请的时候，填写一个 URL，例如：[www.funtl.com]()
- 每次当有用户授权成功之后，认证服务器将页面重定向到这个 URL（回调），并带上 `access_token`，例如：[www.funtl.com?access_token=123456789]()
- 客户端接收到了这个 `access_token`，而且认证服务器的授权动作已经完成，刚好可以把程序的控制权转交回客户端，由客户端决定接下来向用户展示什么内容

# 3.令牌访问与刷新

## 3.1.Access Token

Access Token 是客户端访问资源服务器的令牌。拥有这个令牌代表着得到用户的授权。然而，这个授权应该是 **临时** 的，有一定有效期。这是因为，Access Token 在使用的过程中 **可能会泄露**。给 Access Token 限定一个 **较短的有效期** 可以降低因 Access Token 泄露而带来的风险。

然而引入了有效期之后，客户端使用起来就不那么方便了。每当 Access Token 过期，客户端就必须重新向用户索要授权。这样用户可能每隔几天，甚至每天都需要进行授权操作。这是一件非常影响用户体验的事情。希望有一种方法，可以避免这种情况。

于是 oAuth2.0 引入了 Refresh Token 机制

## 3.2.Refresh Token

Refresh Token 的作用是用来刷新 Access Token。认证服务器提供一个刷新接口，例如：

```
http://www.funtl.com/refresh?refresh_token=&client_id=
```

传入 `refresh_token` 和 `client_id`，认证服务器验证通过后，返回一个新的 Access Token。为了安全，oAuth2.0 引入了两个措施：

- oAuth2.0 要求，Refresh Token **一定是保存在客户端的服务器上** ，而绝不能存放在狭义的客户端（例如 App、PC 端软件）上。调用 `refresh` 接口的时候，一定是从服务器到服务器的访问。
- oAuth2.0 引入了 `client_secret` 机制。即每一个 `client_id` 都对应一个 `client_secret`。这个 `client_secret` 会在客户端申请 `client_id` 时，随 `client_id` 一起分配给客户端。**客户端必须把 `client_secret` 妥善保管在服务器上**，决不能泄露。刷新 Access Token 时，需要验证这个 `client_secret`。

实际上的刷新接口类似于：

```
http://www.funtl.com/refresh?refresh_token=&client_id=&client_secret=
```

以上就是 Refresh Token 机制。Refresh Token 的有效期非常长，会在用户授权时，随 Access Token 一起重定向到回调 URL，传递给客户端。

# 4.客户端授权模式

## 4.1.概述

客户端必须得到用户的授权（authorization grant），才能获得令牌（access token）。oAuth 2.0 定义了四种授权方式。

- implicit：简化模式，不推荐使用
- authorization code：授权码模式
- resource owner password credentials：密码模式
- client credentials：客户端模式

## 4.2.简化模式

简化模式适用于纯静态页面应用。所谓纯静态页面应用，也就是应用没有在服务器上执行代码的权限（通常是把代码托管在别人的服务器上），只有前端 JS 代码的控制权。

这种场景下，应用是没有持久化存储的能力的。因此，按照 oAuth2.0 的规定，这种应用是拿不到 Refresh Token 的。其整个授权流程如下：

![img](images/简化模式.png)

该模式下，`access_token` 容易泄露且不可刷新

## 4.3.授权码模式

授权码模式适用于有自己的服务器的应用，它是一个一次性的临时凭证，用来换取 `access_token` 和 `refresh_token`。认证服务器提供了一个类似这样的接口：

```
https://www.funtl.com/exchange?code=&client_id=&client_secret=
```

需要传入 `code`、`client_id` 以及 `client_secret`。验证通过后，返回 `access_token` 和 `refresh_token`。一旦换取成功，`code` 立即作废，不能再使用第二次。流程图如下：

![img](images/授权码模式.png)

这个 code 的作用是保护 token 的安全性。上一节说到，简单模式下，token 是不安全的。这是因为在第 4 步当中直接把 token 返回给应用。而这一步容易被拦截、窃听。引入了 code 之后，即使攻击者能够窃取到 code，但是由于他无法获得应用保存在服务器的 `client_secret`，因此也无法通过 code 换取 token。而第 5 步，为什么不容易被拦截、窃听呢？这是因为，首先，这是一个从服务器到服务器的访问，黑客比较难捕捉到；其次，这个请求通常要求是 https 的实现。即使能窃听到数据包也无法解析出内容。

有了这个 code，token 的安全性大大提高。因此，oAuth2.0 鼓励使用这种方式进行授权，而简单模式则是在不得已情况下才会使用。

## 4.4.密码模式

密码模式中，用户向客户端提供自己的用户名和密码。客户端使用这些信息，向 "服务商提供商" 索要授权。在这种模式中，用户必须把自己的密码给客户端，但是客户端不得储存密码。这通常用在用户对客户端高度信任的情况下，比如客户端是操作系统的一部分。

一个典型的例子是同一个企业内部的不同产品要使用本企业的 oAuth2.0 体系。在有些情况下，产品希望能够定制化授权页面。由于是同个企业，不需要向用户展示“xxx将获取以下权限”等字样并询问用户的授权意向，而只需进行用户的身份认证即可。这个时候，由具体的产品团队开发定制化的授权界面，接收用户输入账号密码，并直接传递给鉴权服务器进行授权即可。

![img](images/密码模式.png)

有一点需要特别注意的是，在第 2 步中，认证服务器需要对客户端的身份进行验证，确保是受信任的客户端。

## 4.5.客户端模式

如果信任关系再进一步，或者调用者是一个后端的模块，没有用户界面的时候，可以使用客户端模式。鉴权服务器直接对客户端进行身份验证，验证通过后，返回 token。

![img](images/客户端模式.png)

# 5.实战案例

Oauth2授权主要由两部分组成：

- Authorization Server：认证服务
- Resource Server：资源服务

在实际项目中以上两个服务可以在一个服务器上，也可以分开部署，绝大多数都是分开部署的，这里分开部署。

## 5.1.认证服务器

### 5.1.1引入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-oauth2</artifactId>
</dependency>
```

### 5.1.2.Application

```java
@SpringBootApplication
public class Oauth2CaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(Oauth2CaseApplication.class, args);
    }
}
```

### 5.1.3.基于内存存储令牌

本章节基于 **内存存储令牌** 的模式用于演示最基本的操作，快速理解 oAuth2 认证服务器中 "认证"、"授权"、"访问令牌” 的基本概念

#### 1）操作流程

![](images/操作流程.png)

- 配置认证服务器

  - 配置客户端信息：

    ```
    ClientDetailsServiceConfigurer
    ```

    - `inMemory`：内存配置
    - `withClient`：客户端标识
    - `secret`：客户端安全码
    - `authorizedGrantTypes`：客户端授权类型
    - `scopes`：客户端授权范围
    - `redirectUris`：注册回调地址

- 配置 Web 安全

- 通过 GET 请求访问认证服务器获取授权码

  - 端点：`/oauth/authorize`

- 通过 POST 请求利用授权码访问认证服务器获取令牌

  - 端点：`/oauth/token`

#### 2）默认的端点 URL

- `/oauth/authorize`：授权端点
- `/oauth/token`：令牌端点
- `/oauth/confirm_access`：用户确认授权提交端点
- `/oauth/error`：授权服务错误信息端点
- `/oauth/check_token`：用于资源服务访问的令牌解析端点
- `/oauth/token_key`：提供公有密匙的端点，如果你使用 JWT 令牌的话

#### 3）服务器安全配置

创建一个类继承 `WebSecurityConfigurerAdapter` 并添加相关注解：

- `@Configuration`
- `@EnableWebSecurity`
- `@EnableGlobalMethodSecurity(prePostEnabled = true,securedEnabled = true, jsr250Enabled = true)`：全局方法拦截

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true,
                            securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
  
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // 配置默认的加密方式
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 在内存中创建用户
        auth.inMemoryAuthentication()
                .withUser("user")
                .password(passwordEncoder().encode("123456"))
                .roles("USER")
                .and()
                .withUser("admin")
                .password(passwordEncoder().encode("admin888"))
                .roles("ADMIN");
    }
}
```

#### 4）配置认证服务器

创建一个类继承 `AuthorizationServerConfigurerAdapter` 并添加相关注解：

- `@Configuration`
- `@EnableAuthorizationServer`

```java
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // 配置客户端
        clients
                // 使用内存设置
                .inMemory()
                // client_id
                .withClient("client")
                // client_secret
                .secret(passwordEncoder.encode("secret"))
                // 授权类型
                .authorizedGrantTypes("authorization_code")
                // 授权范围
                .scopes("app")
                // 注册回调地址
                .redirectUris("https://www.baidu.com");
    }
}
```

#### 5）application.yml

```yaml
server:
  port: 8080
```

#### 6）访问获取授权码

- 通过浏览器访问

```
http://localhost:8080/oauth/authorize?client_id=client&response_type=code
```

- 第一次访问会跳转到登录页面

![img](images/Spring security 登录界面.png)

- 验证成功后会询问用户是否授权客户端

![img](images/Spring security 询问授权.png)

- 选择授权后会跳转到百度，浏览器地址上还会包含一个授权码（`code=1JuO6V`），浏览器地址栏会显示如下地址：

```
https://www.baidu.com/?code=1JuO6V
```

#### 7）向认证服务器申请令牌

- 通过 CURL 或是 Postman [POST] 请求

```
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'grant_type=authorization_code&code=1JuO6V' 
```

```
http://client:secret@localhost:8080/oauth/token
```

![img](images/postman申请令牌.png)

- 得到响应结果如下

```json
{
  "access_token": "016d8d4a-dd6e-4493-b590-5f072923c413",
  "token_type": "bearer",
  "expires_in": 43199,
  "scope": "app"
}
```

### 5.1.4.基于JDBC存储令牌

本章节 **基于 JDBC 存储令牌** 的模式用于演示最基本的操作，快速理解 oAuth2 认证服务器中 "认证"、"授权"、"访问令牌” 的基本概念

#### 1）操作流程

![img](http://www.qfdmy.com/wp-content/uploads/2019/08/253eb4ef0c03ae7-1.png)

- 初始化 oAuth2 相关表

- 在数据库中配置客户端

- 配置认证服务器

  - 配置数据源：`DataSource`

  - 配置令牌存储方式：`TokenStore` -> `JdbcTokenStore`

  - 配置客户端读取方式：`ClientDetailsService` -> `JdbcClientDetailsService`

  - 配置服务端点信息：

    ```
    AuthorizationServerEndpointsConfigurer
    ```

    - `tokenStore`：设置令牌存储方式

  - 配置客户端信息：

    ```
    ClientDetailsServiceConfigurer
    ```

    - `withClientDetails`：设置客户端配置读取方式

- 配置 Web 安全

  - 配置密码加密方式：`BCryptPasswordEncoder`
  - 配置认证信息：`AuthenticationManagerBuilder`

- 通过 GET 请求访问认证服务器获取授权码

  - 端点：`/oauth/authorize`

- 通过 POST 请求利用授权码访问认证服务器获取令牌

  - 端点：`/oauth/token`

#### 2）默认的端点 URL

- `/oauth/authorize`：授权端点
- `/oauth/token`：令牌端点
- `/oauth/confirm_access`：用户确认授权提交端点
- `/oauth/error`：授权服务错误信息端点
- `/oauth/check_token`：用于资源服务访问的令牌解析端点
- `/oauth/token_key`：提供公有密匙的端点，如果你使用 JWT 令牌的话

#### 3）初始化 oAuth2 相关表

使用官方提供的建表脚本初始化 oAuth2 相关表，地址如下：

[https://github.com/spring-projects/spring-security-oauth/blob/master/spring-security-oauth2/src/test/resources/schema.sql](https://github.com/spring-projects/spring-security-oauth/blob/master/spring-security-oauth2/src/test/resources/schema.sql)

由于我们使用的是 MySQL 数据库，默认建表语句中主键为 `VARCHAR(256)`，这超过了最大的主键长度，请手动修改为 `128`，并用 `BLOB` 替换语句中的 `LONGVARBINARY` 类型，修改后的建表脚本如下：

```mysql
CREATE TABLE `clientdetails` (
  `appId` varchar(128) NOT NULL,
  `resourceIds` varchar(256) DEFAULT NULL,
  `appSecret` varchar(256) DEFAULT NULL,
  `scope` varchar(256) DEFAULT NULL,
  `grantTypes` varchar(256) DEFAULT NULL,
  `redirectUrl` varchar(256) DEFAULT NULL,
  `authorities` varchar(256) DEFAULT NULL,
  `access_token_validity` int(11) DEFAULT NULL,
  `refresh_token_validity` int(11) DEFAULT NULL,
  `additionalInformation` varchar(4096) DEFAULT NULL,
  `autoApproveScopes` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`appId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oauth_access_token` (
  `token_id` varchar(256) DEFAULT NULL,
  `token` blob,
  `authentication_id` varchar(128) NOT NULL,
  `user_name` varchar(256) DEFAULT NULL,
  `client_id` varchar(256) DEFAULT NULL,
  `authentication` blob,
  `refresh_token` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`authentication_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oauth_approvals` (
  `userId` varchar(256) DEFAULT NULL,
  `clientId` varchar(256) DEFAULT NULL,
  `scope` varchar(256) DEFAULT NULL,
  `status` varchar(10) DEFAULT NULL,
  `expiresAt` timestamp NULL DEFAULT NULL,
  `lastModifiedAt` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oauth_client_details` (
  `client_id` varchar(128) NOT NULL,
  `resource_ids` varchar(256) DEFAULT NULL,
  `client_secret` varchar(256) DEFAULT NULL,
  `scope` varchar(256) DEFAULT NULL,
  `authorized_grant_types` varchar(256) DEFAULT NULL,
  `web_server_redirect_uri` varchar(256) DEFAULT NULL,
  `authorities` varchar(256) DEFAULT NULL,
  `access_token_validity` int(11) DEFAULT NULL,
  `refresh_token_validity` int(11) DEFAULT NULL,
  `additional_information` varchar(4096) DEFAULT NULL,
  `autoapprove` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oauth_client_token` (
  `token_id` varchar(256) DEFAULT NULL,
  `token` blob,
  `authentication_id` varchar(128) NOT NULL,
  `user_name` varchar(256) DEFAULT NULL,
  `client_id` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`authentication_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oauth_code` (
  `code` varchar(256) DEFAULT NULL,
  `authentication` blob
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `oauth_refresh_token` (
  `token_id` varchar(256) DEFAULT NULL,
  `token` blob,
  `authentication` blob
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

#### 4）在数据库中配置客户端

在表 `oauth_client_details` 中增加一条客户端配置记录，需要设置的字段如下：

- `client_id`：客户端标识
- `client_secret`：客户端安全码，此处不能是明文，需要加密
- `scope`：客户端授权范围
- `authorized_grant_types`：客户端授权类型
- `web_server_redirect_uri`：服务器回调地址

使用 `BCryptPasswordEncoder` 为客户端安全码加密，代码如下：

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class EncoderTest {

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Test
    public void encoderPassword(){
        System.out.println(passwordEncoder.encode("secret"));
    }
}
```

#### 5）pom.xml 新增依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

#### 6）application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    jdbc-url: jdbc:mysql://localhost:3306/mdzz_seckill
    username: root
    password: 12345678
    hikari:
      minimum-idle: 5
      idle-timeout: 600000
      maximum-pool-size: 10
      auto-commit: true
      pool-name: MyHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
```

#### 7）配置认证服务器

```java
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 数据源中的 jdbc-url 需要 oauth 来解析
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public TokenStore tokenStore() {
        // 基于 JDBC 实现，令牌保存到数据库
        return new JdbcTokenStore(dataSource());
    }

    @Bean
    public ClientDetailsService jdbcClientDetailsService() {
        // 基于 JDBC 实现，需要事先在数据库配置客户端信息
        return new JdbcClientDetailsService(dataSource());
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer 
                          endpoints) throws Exception {
        // 设置令牌存储模式
        endpoints.tokenStore(tokenStore());
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // 客户端配置
        clients.withClientDetails(jdbcClientDetailsService());
    }

}
```

#### 8）访问获取授权码

- 通过浏览器访问

```
http://localhost:8080/oauth/authorize?client_id=client&response_type=code
```

- 第一次访问会跳转到登录页面

![img](images/Spring security 登录界面.png)

- 验证成功后会询问用户是否授权客户端

![img](images/Spring security 询问授权.png)

- 选择授权后会跳转到百度，浏览器地址上还会包含一个授权码（`code=1JuO6V`），浏览器地址栏会显示如下地址：

```
https://www.baidu.com/?code=1JuO6V
```

#### 9）向认证服务器申请令牌

- 通过 CURL 或是 Postman [POST] 请求

```
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'grant_type=authorization_code&code=1JuO6V' 
```

```
http://client:secret@localhost:8080/oauth/token
```

![img](images/postman申请令牌.png)

- 得到响应结果如下

```json
{
  "access_token": "016d8d4a-dd6e-4493-b590-5f072923c413",
  "token_type": "bearer",
  "expires_in": 43199,
  "scope": "app"
}
```

### 5.1.5.RBAC基于角色的访问控制

RBAC（Role-Based Access Control，基于角色的访问控制），就是用户通过角色与权限进行关联。简单地说，一个用户拥有若干角色，每一个角色拥有若干权限。这样，就构造成“用户-角色-权限”的授权模型。在这种模型中，用户与角色之间，角色与权限之间，一般是多对多的关系。（如下图）

![img](images/RBAC表结构.png)

#### 1）目的

在我们的 oAuth2 系统中，我们需要对系统的所有资源进行权限控制，系统中的资源包括：

- 静态资源（对象资源）：功能操作、数据列
- 动态资源（数据资源）：数据

系统的目的就是对应用系统的所有对象资源和数据资源进行权限控制，比如：功能菜单、界面按钮、数据显示的列、各种行级数据进行权限的操控

#### 2）对象关系

##### 2.1）权限

系统的所有权限信息。权限具有上下级关系，是一个树状的结构。如：

- 系统管理
  - 用户管理
    - 查看用户
    - 新增用户
    - 修改用户
    - 删除用户

##### 2.2）用户

系统的具体操作者，可以归属于一个或多个角色，它与角色的关系是多对多的关系

##### 2.3）角色

为了对许多拥有相似权限的用户进行分类管理，定义了角色的概念，例如系统管理员、管理员、用户、访客等角色。角色具有上下级关系，可以形成树状视图，父级角色的权限是自身及它的所有子角色的权限的综合。父级角色的用户、父级角色的组同理可推。

##### 2.4）关系图

![img](images/RBAC关系图.png)

##### 2.5）模块图

![img](images/RBAC模块图.png)

#### 3）表结构

```mysql
CREATE TABLE `tb_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父权限',
  `name` varchar(64) NOT NULL COMMENT '权限名称',
  `enname` varchar(64) NOT NULL COMMENT '权限英文名称',
  `url` varchar(255) NOT NULL COMMENT '授权路径',
  `description` varchar(200) DEFAULT NULL COMMENT '备注',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8 COMMENT='权限表';

CREATE TABLE `tb_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父角色',
  `name` varchar(64) NOT NULL COMMENT '角色名称',
  `enname` varchar(64) NOT NULL COMMENT '角色英文名称',
  `description` varchar(200) DEFAULT NULL COMMENT '备注',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8 COMMENT='角色表';

CREATE TABLE `tb_role_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  `permission_id` bigint(20) NOT NULL COMMENT '权限 ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8 COMMENT='角色权限表';

CREATE TABLE `tb_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(64) NOT NULL COMMENT '密码，加密存储',
  `phone` varchar(20) DEFAULT NULL COMMENT '注册手机号',
  `email` varchar(50) DEFAULT NULL COMMENT '注册邮箱',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`) USING BTREE,
  UNIQUE KEY `phone` (`phone`) USING BTREE,
  UNIQUE KEY `email` (`email`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8 COMMENT='用户表';

CREATE TABLE `tb_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8 COMMENT='用户角色表';
```

### 5.1.6.基于 RBAC 的自定义认证

在实际开发中，我们的用户信息都是存在数据库里的，本章节基于 **RBAC 模型** 将用户的认证信息与数据库对接，实现真正的用户认证与授权

#### 1）操作流程

继续 **基于 JDBC 存储令牌** 章节的代码开发

- 初始化 RBAC 相关表
- 在数据库中配置“用户”、“角色”、“权限”相关信息
- 数据库操作使用 `mybatis` 框架，故需要增加相关依赖
- 配置 Web 安全
  - 配置使用自定义认证与授权
- 通过 GET 请求访问认证服务器获取授权码
  - 端点：`/oauth/authorize`
- 通过 POST 请求利用授权码访问认证服务器获取令牌
  - 端点：`/oauth/token`

#### 2）默认的端点 URL

- `/oauth/authorize`：授权端点
- `/oauth/token`：令牌端点
- `/oauth/confirm_access`：用户确认授权提交端点
- `/oauth/error`：授权服务错误信息端点
- `/oauth/check_token`：用于资源服务访问的令牌解析端点
- `/oauth/token_key`：提供公有密匙的端点，如果你使用 JWT 令牌的话

#### 3）初始化 RBAC 相关表

```mysql
CREATE TABLE `tb_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父权限',
  `name` varchar(64) NOT NULL COMMENT '权限名称',
  `enname` varchar(64) NOT NULL COMMENT '权限英文名称',
  `url` varchar(255) NOT NULL COMMENT '授权路径',
  `description` varchar(200) DEFAULT NULL COMMENT '备注',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8 COMMENT='权限表';

insert into `tb_permission`(`id`,`parent_id`,`name`,`enname`,`url`,`description`,`created`,`updated`) values 
(37,0,'系统管理','System','/',NULL,'2019-04-04 23:22:54','2019-04-04 23:22:56'),
(38,37,'用户管理','SystemUser','/users/',NULL,'2019-04-04 23:25:31','2019-04-04 23:25:33'),
(39,38,'查看用户','SystemUserView','',NULL,'2019-04-04 15:30:30','2019-04-04 15:30:43'),
(40,38,'新增用户','SystemUserInsert','',NULL,'2019-04-04 15:30:31','2019-04-04 15:30:44'),
(41,38,'编辑用户','SystemUserUpdate','',NULL,'2019-04-04 15:30:32','2019-04-04 15:30:45'),
(42,38,'删除用户','SystemUserDelete','',NULL,'2019-04-04 15:30:48','2019-04-04 15:30:45');

CREATE TABLE `tb_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父角色',
  `name` varchar(64) NOT NULL COMMENT '角色名称',
  `enname` varchar(64) NOT NULL COMMENT '角色英文名称',
  `description` varchar(200) DEFAULT NULL COMMENT '备注',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8 COMMENT='角色表';

insert into `tb_role`(`id`,`parent_id`,`name`,`enname`,`description`,`created`,`updated`) values 
(37,0,'超级管理员','admin',NULL,'2019-04-04 23:22:03','2019-04-04 23:22:05');

CREATE TABLE `tb_role_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  `permission_id` bigint(20) NOT NULL COMMENT '权限 ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8 COMMENT='角色权限表';

insert into `tb_role_permission`(`id`,`role_id`,`permission_id`) values 
(37,37,37),
(38,37,38),
(39,37,39),
(40,37,40),
(41,37,41),
(42,37,42);

CREATE TABLE `tb_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(64) NOT NULL COMMENT '密码，加密存储',
  `phone` varchar(20) DEFAULT NULL COMMENT '注册手机号',
  `email` varchar(50) DEFAULT NULL COMMENT '注册邮箱',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`) USING BTREE,
  UNIQUE KEY `phone` (`phone`) USING BTREE,
  UNIQUE KEY `email` (`email`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8 COMMENT='用户表';

insert into `tb_user`(`id`,`username`,`password`,`phone`,`email`,`created`,`updated`) values 
(37,'admin','$2a$10$9ZhDOBp.sRKat4l14ygu/.LscxrMUcDAfeVOEPiYwbcRkoB09gCmi','15888888888','lee.lusifer@gmail.com','2019-04-04 23:21:27','2019-04-04 23:21:29');

CREATE TABLE `tb_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8 COMMENT='用户角色表';

insert into `tb_user_role`(`id`,`user_id`,`role_id`) values 
(37,37,37);
```

#### 4）pom.xml 新增依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.1.0</version>
</dependency>
```

#### 5）使用 mybatis-generator 生成代码

生成`tb_permission，tb_user，tb_role` 三张表的 `mapper.xml、mapper.class、entity`；

`TbPermissionMapper` 类中新增：

```java
List<TbPermission> selectByUserId(@Param("userId") Long userId);
```

对应的 xml 中新增：

```xml
<select id="selectByUserId" resultMap="BaseResultMap">
    SELECT
      p.*
    FROM
      tb_user AS u
      LEFT JOIN tb_user_role AS ur
        ON u.id = ur.user_id
      LEFT JOIN tb_role AS r
        ON r.id = ur.role_id
      LEFT JOIN tb_role_permission AS rp
        ON r.id = rp.role_id
      LEFT JOIN tb_permission AS p
        ON p.id = rp.permission_id
    WHERE u.id = ${userId}
</select>
```

`TbUserMapper` 类中新增：

```java
TbUser selectUserByName(String username);
```

对应的 xml 中新增：

```xml
<select id="selectUserByName" resultMap="BaseResultMap">
    select
    <include refid="Base_Column_List" />
    from tb_user
    where username = #{username}
</select>
```

`TbRoleMapper` 类不做改变。

#### 6）新增 service 类

```java
@Service
public class TbPermissionService {

    @Resource
    private TbPermissionMapper tbPermissionMapper;

    public List<TbPermission> selectByUserId(Long userId) {
        return tbPermissionMapper.selectByUserId(userId);
    }
}
```

```java
@Service
public class TbUserService {

    @Resource
    private TbUserMapper tbUserMapper;

    public TbUser getByUsername(String username) {
        return tbUserMapper.selectUserByName(username);
    }
}
```

无需新增 TbRoleService 类。

#### 7）application.yml 新增

```yaml
mybatis:
  type-aliases-package: com.example.oauth2case.domain
  mapper-locations: classpath:mapper/*.xml
```

#### 8）Application 启动类新增

```java
@MapperScan(basePackages = "com.example.oauth2case.mapper")
```

#### 9）自定义认证授权实现类

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private TbUserService tbUserService;
    @Autowired
    private TbPermissionService tbPermissionService;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        TbUser tbUser = tbUserService.getByUsername(s);
        List<GrantedAuthority> grantedAuthorities = Lists.newArrayList();
        if (tbUser != null) {
            // 声明用户授权
            List<TbPermission> tbPermissions = tbPermissionService
                      .selectByUserId(tbUser.getId());
            tbPermissions.forEach(tbPermission -> {
                GrantedAuthority grantedAuthority = 
                      new SimpleGrantedAuthority(tbPermission.getEnname());
                grantedAuthorities.add(grantedAuthority);
            });
            // 由框架完成认证工作
            return new User(tbUser.getUsername(), tbUser.getPassword(), grantedAuthorities);
        }
        return null;
    }
}
```

#### 10）服务器安全配置

创建一个类继承 `WebSecurityConfigurerAdapter` 并添加相关注解：

- `@Configuration`
- `@EnableWebSecurity`
- `@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)`：全局方法拦截

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // 配置默认的加密方式
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    protected UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 使用自定义认证与授权
        auth.userDetailsService(userDetailsService());
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 将 check_token 暴露出去，否则资源服务器访问时报 403 错误
        web.ignoring().antMatchers("/oauth/check_token");
    }

}
```

#### 11）访问获取授权码

- 通过浏览器访问

```
http://localhost:8080/oauth/authorize?client_id=client&response_type=code
```

- 第一次访问会跳转到登录页面

![img](images/Spring security 登录界面.png)

- 验证成功后会询问用户是否授权客户端

![img](images/Spring security 询问授权.png)

- 选择授权后会跳转到百度，浏览器地址上还会包含一个授权码（`code=1JuO6V`），浏览器地址栏会显示如下地址：

```
https://www.baidu.com/?code=1JuO6V
```

#### 12）向认证服务器申请令牌

- 通过 CURL 或是 Postman [POST] 请求

```
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'grant_type=authorization_code&code=1JuO6V' 
```

```
http://client:secret@localhost:8080/oauth/token
```

![img](images/postman申请令牌.png)

- 得到响应结果如下

```json
{
  "access_token": "016d8d4a-dd6e-4493-b590-5f072923c413",
  "token_type": "bearer",
  "expires_in": 43199,
  "scope": "app"
}
```

操作成功后数据库 `oauth_access_token` 表中会增加一笔记录，效果图如下：

![](images/RBAC认证成功效果图.png)

## 5.2.资源服务器

### 5.2.1.概述

在 **为什么需要 oAuth2** 和 **RBAC 基于角色的权限控制** 章节，我们介绍过资源的概念，简单点说就是需要被访问的业务数据或是静态资源文件都可以被称作资源。

为了更好的理解资源服务器的概念，我们单独创建一个名为 `resource-case` 资源服务器的项目，该项目的主要目的就是对数据表的 CRUD 操作，而这些操作就是对资源的操作了。

**操作流程**

![img](http://www.qfdmy.com/wp-content/uploads/2019/08/901ef32a4fa6cad.png)

- 初始化资源服务器数据库
- POM 所需依赖同认证服务器
- 配置资源服务器
- 配置资源(Controller)

### 5.2.2.初始化资源服务器数据库

```mysql
CREATE TABLE `tb_content` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) NOT NULL COMMENT '内容类目ID',
  `title` varchar(200) DEFAULT NULL COMMENT '内容标题',
  `sub_title` varchar(100) DEFAULT NULL COMMENT '子标题',
  `title_desc` varchar(500) DEFAULT NULL COMMENT '标题描述',
  `url` varchar(500) DEFAULT NULL COMMENT '链接',
  `pic` varchar(300) DEFAULT NULL COMMENT '图片绝对路径',
  `pic2` varchar(300) DEFAULT NULL COMMENT '图片2',
  `content` text COMMENT '内容',
  `created` datetime DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `updated` (`updated`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8;
insert  into `tb_content`(`id`,`category_id`,`title`,`sub_title`,`title_desc`,`url`,`pic`,`pic2`,`content`,`created`,`updated`) values 
(28,89,'标题','子标题','标题说明','http://www.jd.com',NULL,NULL,NULL,'2019-04-07 00:56:09','2019-04-07 00:56:11'),
(29,89,'ad2','ad2','ad2','http://www.baidu.com',NULL,NULL,NULL,'2019-04-07 00:56:13','2019-04-07 00:56:15'),
(30,89,'ad3','ad3','ad3','http://www.sina.com.cn',NULL,NULL,NULL,'2019-04-07 00:56:17','2019-04-07 00:56:19'),
(31,89,'ad4','ad4','ad4','http://www.funtl.com',NULL,NULL,NULL,'2019-04-07 00:56:22','2019-04-07 00:56:25');
CREATE TABLE `tb_content_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类目ID',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父类目ID=0时，代表的是一级的类目',
  `name` varchar(50) DEFAULT NULL COMMENT '分类名称',
  `status` int(1) DEFAULT '1' COMMENT '状态。可选值:1(正常),2(删除)',
  `sort_order` int(4) DEFAULT NULL COMMENT '排列序号，表示同级类目的展现次序，如数值相等则按名称次序排列。取值范围:大于零的整数',
  `is_parent` tinyint(1) DEFAULT '1' COMMENT '该类目是否为父类目，1为true，0为false',
  `created` datetime DEFAULT NULL COMMENT '创建时间',
  `updated` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`,`status`) USING BTREE,
  KEY `sort_order` (`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=98 DEFAULT CHARSET=utf8 COMMENT='内容分类';
insert  into `tb_content_category`(`id`,`parent_id`,`name`,`status`,`sort_order`,`is_parent`,`created`,`updated`) values 
(30,0,'LeeShop',1,1,1,'2015-04-03 16:51:38','2015-04-03 16:51:40'),
(86,30,'首页',1,1,1,'2015-06-07 15:36:07','2015-06-07 15:36:07'),
(87,30,'列表页面',1,1,1,'2015-06-07 15:36:16','2015-06-07 15:36:16'),
(88,30,'详细页面',1,1,1,'2015-06-07 15:36:27','2015-06-07 15:36:27'),
(89,86,'大广告',1,1,0,'2015-06-07 15:36:38','2015-06-07 15:36:38'),
(90,86,'小广告',1,1,0,'2015-06-07 15:36:45','2015-06-07 15:36:45'),
(91,86,'商城快报',1,1,0,'2015-06-07 15:36:55','2015-06-07 15:36:55'),
(92,87,'边栏广告',1,1,0,'2015-06-07 15:37:07','2015-06-07 15:37:07'),
(93,87,'页头广告',1,1,0,'2015-06-07 15:37:17','2015-06-07 15:37:17'),
(94,87,'页脚广告',1,1,0,'2015-06-07 15:37:31','2015-06-07 15:37:31'),
(95,88,'边栏广告',1,1,0,'2015-06-07 15:37:56','2015-06-07 15:37:56'),
(96,86,'中广告',1,1,1,'2015-07-25 18:58:52','2015-07-25 18:58:52'),
(97,96,'中广告1',1,1,0,'2015-07-25 18:59:43','2015-07-25 18:59:43');
```

### 5.2.3.pom.xml 依赖

此处依赖同认证服务器，不再赘述。

### 5.2.4.application.yml

```yaml
server:
  port: 8081
  servlet:
    context-path: /contents

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mdzz_seckill
    username: root
    password: 12345678
    hikari:
      minimum-idle: 5
      idle-timeout: 600000
      maximum-pool-size: 10
      auto-commit: true
      pool-name: MyHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1

mybatis:
  type-aliases-package: com.example.resourceCase.entity
  mapper-locations: classpath:mappers/*.xml

security:
  oauth2:
    client:
      client-id: client
      client-secret: secret
      access-token-uri: http://localhost:8080/oauth/token
      user-authorization-uri: http://localhost:8080/oauth/authorize
    resource:
      token-info-uri: http://localhost:8080/oauth/check_token

logging:
  level:
    root: INFO
    org.springframework.web: INFO
    org.springframework.security: INFO
    org.springframework.security.oauth2: INFO
```

### 5.2.5.Application启动类

```java
@SpringBootApplication
@MapperScan("com.example.resourceCase.mapper")
public class ResourceCaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResourceCaseApplication.class, args);
    }
}
```

### 5.2.6.配置资源服务器

创建一个类继承 `ResourceServerConfigurerAdapter` 并添加相关注解：

- `@Configuration`
- `@EnableResourceServer`：资源服务器
- `@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)`：全局方法拦截

```java
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .exceptionHandling()
                .and()
                // Session 创建策略
                // ALWAYS 总是创建 HttpSession
                // IF_REQUIRED Spring Security 只会在需要时创建一个 HttpSession
                // NEVER Spring Security 不会创建 HttpSession，但如果它已经存在，将可以使用 HttpSession
                // STATELESS Spring Security 永远不会创建 HttpSession，它不会使用 HttpSession 来获取 SecurityContext
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 以下为配置所需保护的资源路径及权限，需要与认证服务器配置的授权部分对应
                .antMatchers("/").hasAuthority("SystemContent")
                .antMatchers("/view/**").hasAuthority("SystemContentView")
                .antMatchers("/insert/**").hasAuthority("SystemContentInsert")
                .antMatchers("/update/**").hasAuthority("SystemContentUpdate")
                .antMatchers("/delete/**").hasAuthority("SystemContentDelete");
    }
}
```

### 5.2.7.数据传输对象

创建一个名为 `ResponseResult` 的通用数据传输对象

```java
@Data
public class ResponseResult<T> implements Serializable {
    private static final long serialVersionUID = 3468352004150968551L;
    /**
     * 状态码
     */
    private Integer state;
    /**
     * 消息
     */
    private String message;
    /**
     * 返回对象
     */
    private T data;

    public ResponseResult() {
        super();
    }

    public ResponseResult(Integer state) {
        super();
        this.state = state;
    }

    public ResponseResult(Integer state, String message) {
        super();
        this.state = state;
        this.message = message;
    }

    public ResponseResult(Integer state, Throwable throwable) {
        super();
        this.state = state;
        this.message = throwable.getMessage();
    }

    public ResponseResult(Integer state, T data) {
        super();
        this.state = state;
        this.data = data;
    }

    public ResponseResult(Integer state, String message, T data) {
        super();
        this.state = state;
        this.message = message;
        this.data = data;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResponseResult<?> other = (ResponseResult<?>) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
            return false;
        }
        if (state == null) {
            if (other.state != null) {
                return false;
            }
        } else if (!state.equals(other.state)) {
            return false;
        }
        return true;
    }
}
```

### 5.2.8.使用 mybatis-generator 生成代码

生成 `tb_content` `tb_content_category` 两张表的 entity 以及 mapper。

`TbContentMapper` 中新增：

```java
List<TbContent> selectAll();
```

对应的 xml 中新增：

```xml
<select id="selectAll" resultType="com.example.resourceCase.entity.TbContent">
    select
    <include refid="Base_Column_List" />
    ,
    <include refid="Blob_Column_List" />
    from tb_content
</select>
```

### 5.2.9.新增 service 类

```java
@Service
public class TbContentService {

    @Resource
    private TbContentMapper tbContentMapper;

    public TbContent getById(Long id) {
        return tbContentMapper.selectByPrimaryKey(id);
    }

    public List<TbContent> selectAll() {
        return tbContentMapper.selectAll();
    }

    public int insert(TbContent tbContent) {
        return tbContentMapper.insert(tbContent);
    }

    public int update(TbContent tbContent) {
        return tbContentMapper.updateByPrimaryKey(tbContent);
    }

    public int delete(Long id) {
        return tbContentMapper.deleteByPrimaryKey(id);
    }
}
```

### 5.2.10.controller 测试类

```java
@RestController
public class TbContentController {
    @Autowired
    private TbContentService tbContentService;
    @GetMapping(value = "/")
    public ResponseResult<List<TbContent>> list() {
        List<TbContent> tbContents = tbContentService.selectAll();
        return new ResponseResult<List<TbContent>>(HttpStatus.OK.value(), 
                                                   HttpStatus.OK.toString(),
                                                   tbContents);
    }
}
```

### 5.2.11.访问获取授权码

- 通过浏览器访问

```
http://localhost:8080/oauth/authorize?client_id=client&response_type=code
```

- 第一次访问会跳转到登录页面

![img](images/Spring security 登录界面.png)

- 验证成功后会询问用户是否授权客户端

![img](images/Spring security 询问授权.png)

- 选择授权后会跳转到百度，浏览器地址上还会包含一个授权码（`code=1JuO6V`），浏览器地址栏会显示如下地址：

```
https://www.baidu.com/?code=1JuO6V
```

### 5.2.12.向服务器申请令牌

- 通过 CURL 或是 Postman 请求

```
curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'grant_type=authorization_code&code=1JuO6V' "http://client:secret@localhost:8080/oauth/token"
```

![img](images/postman申请令牌.png)

- 得到响应结果如下

```json
{
  "access_token": "016d8d4a-dd6e-4493-b590-5f072923c413",
  "token_type": "bearer",
  "expires_in": 43199,
  "scope": "app"
}
```

### 5.2.13.携带令牌访问资源服务器

此处以获取全部资源为例，其它请求方式一样，可以参考我源码中的单元测试代码。可以使用以下方式请求：

- 使用 Headers 方式：需要在请求头增加 `Authorization: Bearer yourAccessToken`
- 直接请求带参数方式：`http://localhost:8081/contents?access_token=yourAccessToken`

使用 Headers 方式，通过 CURL 或是 Postman 请求

```
curl --location --request GET "http://localhost:8081/contents" --header "Content-Type: application/json" --header "Authorization: Bearer yourAccessToken"
```

![img](images/携带令牌访问资源服务器.png)