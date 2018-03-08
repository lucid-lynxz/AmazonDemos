# 亚马逊登录demo

[文档](https://developer.amazon.com/docs/login-with-amazon/install-sdk-android.html)

## 前置条件
### 1. 配置签名文件
本项目将签名参数配置在独立文件 `app/sign.gradle` 中，签名文件也放置于 `app/***.jks` ,此两份文件名并未上传到git中，请自行添加
```gradle
// app/sign.gradle
android{
    signingConfigs {
        releaseConfig {
            keyAlias '***'
            keyPassword '***'
            storeFile file('***.jks')
            storePassword '***'
        }
    }
}
```
### 2. 申请 apiKey
* 根据自己的app包名和签名文件的MD5/SHA256(网页上有提示)，创建 [`Security Profile`](https://developer.amazon.com/lwa/sp/overview.html)
* 切换到 `login with amazon` tab标签页，把刚才添加的 profile 添加的login功能中；

### 3. 结合上面创建的`Security Profile`，创建 product
在 `Alexa` tab页中，选择 `Alexa Voice Service - android/kindle`, 然后 `create product`，按需操作
    P.S.  [开发者product界面](https://developer.amazon.com/avs/home.html#/avs/home) `create product`，按需操作
* 把生成的 Product  `Manage - Security Profile - android/kindle` 信息中的 `API key` 信息(基于时间动态变化，任意一个都可以使用)，并写入 `app/src/main/assets/api_key.txt` 文件中;
    P.S. `api_key.txt` 文件我并没有添加到git中，请自行申请后添加，该信息跟签名文件(`***.jks`)以及包名等信息相关，即使上传了其他人也用不了；
    P.S. api_key 的获取操作请查看 [文档Register for Login with Amazon](https://developer.amazon.com/docs/login-with-amazon/register-android.html)
P.S. 后续需要用到生成的 `Product ID`

## 集成
### 1. [下载sdk文件](https://developer.amazon.com/sdk-download)
### ２. 添加权限
```xml
<!-- androidManifest.xml-->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```
### 3. 添加 Configuration Changes 
在要进行amazon登录的 `Activity` 配置中添加如下信息：
```xml
<!-- androidManifest.xml-->
<activity
    android:configChanges="keyboard|keyboardHidden|orientation|screenSize">
</activity>
```

### 4. 添加 `WorkflowActivity` 信息
由于app进行amazon帐号登录的时候，需要调起浏览器，因此需要添加 `WorkflowActivity`
注意：请把下方的 `${applicationId}` 替换为gradle文件中制定的 `applicationId`;
```xml
<!-- androidManifest.xml-->
<activity
    android:name="com.amazon.identity.auth.device.workflow.WorkflowActivity"
    android:allowTaskReparenting="true"
    android:launchMode="singleTask"
    android:theme="@android:style/Theme.NoDisplay">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>

        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
        <!-- android:host must use the full package name found in Manifest General Attributes -->
        <data
            android:host="${applicationId}"
            android:scheme="amzn"/>
    </intent-filter>
</activity>
```
## 代码使用
### 1. 创建授权监听回调
```kotlin
private val authListener = object : AuthorizeListener() {
    override fun onSuccess(result: AuthorizeResult?) {
        // 登录成功后，可以获取相关操作，包括获取个人资料等
        fetchUserData(result)
    }

    override fun onCancel(cancellation: AuthCancellation?) {
        Logger.d("登录取消 ${cancellation?.description}")
    }

    override fun onError(ae: AuthError?) {
        Logger.e("authrize error: ${ae?.localizedMessage}")
    }
}

// 授权请求操作 RequestContext 文档是建议在 Activity 的 onCreate() 或者 fragment 的 onViewCreated() 中创建
private val requestContext by lazy {
    RequestContext.create(this).apply {
        registerListener(authListener)
    }
}
```

### 2. 设置授权登录request信息(包括要获取的信息scope)等，然后授权
```kotlin
private fun loginWithAmazon() {
    // authorize() 是异步方法，不用重新创建子线程
    try {
        AuthorizationManager.authorize(
                AuthorizeRequest
                        .Builder(requestContext)
                        .addScopes(ProfileScope.postalCode(), ProfileScope.profile(), ProfileScope.userId())
                        .build()
        )
    } catch (e: IllegalArgumentException) {
        // 一般是 API KEY 不正确
        showToast("登录失败:${e.message}")
        e.printStackTrace()
    }
}
```

### 3. 授权成功后获取用户的信息
```kotlin
private fun fetchUserData(result: AuthorizeResult?) {
    result?.let {
        User.fetch(this, object : Listener<User, AuthError> {
            override fun onSuccess(user: User?) {
                runOnUiThread {
                    // 可获取包括用户名 ， userId， email 等信息
                    updateProfileData(user)
                }
            }

            override fun onError(ae: AuthError?) {
                showToast("获取用户信息失败")
                Logger.e("获取个人信息失败： ${ae?.describeContents()}")
            }
        })
    }
}
```