package org.lynxz.amazonlogindesmo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.*
import com.amazon.identity.auth.device.api.workflow.RequestContext
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    /**
     * [官方文档](https://developer.amazon.com/docs/login-with-amazon/use-sdk-android.html)
     * 文档建议在 `Activity` 的 `onCreate()` 或者 `Fragment` 的 `onViewCreated()` 方法中创建 AuthorizeListener
     * */
    private val requestContext by lazy {
        RequestContext.create(this).apply {
            registerListener(authListener)
        }
    }

    /**
     * 登录amazon帐号结果回调
     * */
    private val authListener = object : AuthorizeListener() {
        override fun onSuccess(result: AuthorizeResult?) {
            // 登录成功后，可以获取相关操作，包括获取个人资料等
            fetchUserData(result)
        }

        override fun onCancel(cancellation: AuthCancellation?) {
            showToast("您取消了登录")
            Logger.d("登录取消 ${cancellation?.description}")
        }

        override fun onError(ae: AuthError?) {
            showToast("登录失败")
            Logger.e("authrize error: ${ae?.localizedMessage}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_login.setOnClickListener {
            loginWithAmazon()
        }
    }

    override fun onResume() {
        super.onResume()
        requestContext.onResume()
    }

    /**
     * 登录amazon等号，获取相关嘻嘻你
     * 关于可获取的scope信息，请参考 [文档](https://developer.amazon.com/docs/login-with-amazon/use-sdk-android.html)
     * 简要记录如下：
     * <ol>
     * <li>profile (gives access to the user’s name, email address, and Amazon account ID)</li>
     * <li>profile:user_id (gives access to the user’s Amazon account ID only)</li>
     * <li>postal_code (gives access to the user’s zip/postal code on file for their Amazon account).</li>
     * </ol>
     * */
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
            showToast("登录失败:${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 登录成功后获取用户信息
     * */
    private fun fetchUserData(result: AuthorizeResult?) {
        result?.let {
            User.fetch(this, object : Listener<User, AuthError> {
                override fun onSuccess(user: User?) {
                    runOnUiThread {
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

    private fun updateProfileData(user: User?) {
        user?.let {
            tv_info.text = "获取到的用户信息:\n${user.userName}\n${user.userId}\n${user.userEmail}\n${user.userPostalCode}"
        }
    }
}
