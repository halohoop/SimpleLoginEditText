# SimpleLoginEditText
![Android CI](https://github.com/halohoop/SimpleLoginEditText/workflows/Android%20CI/badge.svg)

```
repositories {
    jcenter()
}

dependencies {
    implementation 'com.halohoop:simpleloginet:lastest.version'
}
```

1. 图示

| 左滑清空 | 明/密文码切换 | 自定义icon | tips提示 |
| --- | --- | --- | --- |
| ![图1 左滑删除](https://cdn.jsdelivr.net/gh/halohoop/cdn@latest/github/simpleloginet01.gif) | ![图2](https://cdn.jsdelivr.net/gh/halohoop/cdn@latest/github/simpleloginet02.png) | ![图3](https://cdn.jsdelivr.net/gh/halohoop/cdn@latest/github/simpleloginet03.png) | ![图4](https://cdn.jsdelivr.net/gh/halohoop/cdn@latest/github/simpleloginet04.png) |


2. TODOList Gists
    1. 使用GithubAction+Gradle插件将包发布到jcenter；
    2. 封装清空触发条件；
    3. 封装点击明/密文切换触发条件；
    4. 控制tips提示的时机；
    5. 控件属性参数化(TypedArray)；
    6. 更多细节
        1. 版本适配，AppCompat；
        2. 禁用setError功能；
        3. 首次上传发布到jcenter不能够立即在jcenter的maven库中找到，还需要申请发布一次；
3. TODOList Gists正文
    1. "使用GithubAction+Gradle插件将包发布到jcenter"
        ![Github Actions](https://cdn.jsdelivr.net/gh/halohoop/cdn/github/20200514011221.png)
        Github Actions [中文链接](https://help.github.com/cn/actions) [英文链接](https://help.github.com/en/actions) 相信关注Github的都不陌生。从去年（2019）11月全面发布至今，已有大量攻城狮在上面定义并且分享自己的Workflow，这里默认大家都了解过就一笔带过了。
        此项目当然也拥有自己的Actions，从构建，收集产物，发布Release，到最后publish到jcenter，都只需要通过定义一个自动流水线（Workflow(Workflow包含多个Actions)）即可完成所有工序。
        以下是对本项目Workflow的yml配置文件的介绍，此Workflow的定义可以简单概括为两大步骤：
        1. 设定触发条件；
        2. 指定执行环境和定义流水线步骤；
        ```yaml
        name: Android CI # 定义workflow名称
        # 1. 设定触发条件；
        on: # workflow触发条件，以下设置多个触发条件
          push:
            branches: [ master ] # 在master分支上发生push行为的时候开始执行
            tags: # 满足以下格式的tag发布到远程仓库中的时候开始执行
              - 'v*.*'
              - 'v*.*.rc*'
              - 'v*.*.dev*'
        # 2. 指定执行环境 和 定义流水线步骤；
        jobs: # 定义workflow所有相关配置
          build:
            # 指定执行环境；
            runs-on: ubuntu-latest # 定义运行环境
            # 定义流水线步骤；
            steps: # 定义workflow所有需要执行的步骤
            # 步骤1：checkout代码
            - uses: actions/checkout@v2
            # 步骤2：设置执行的Java环境
            - name: set up JDK 1.8
              uses: actions/setup-java@v1
              with:
                java-version: 1.8
            # 步骤3：执行打包构建得到构建产物
            - name: Build
              run: ./gradlew :lib-simpleloginet:assembleRelease
            # 步骤4：收集构建产物
            - name: Release
              uses: softprops/action-gh-release@v1
              if: startsWith(github.ref, 'refs/tags/')
              with:
                files: |
                  ./lib-simpleloginet/build/outputs/aar/lib-simpleloginet-release.aar
              env:
                GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
            # 步骤5：上传发布到jcenter
            - name: BintrayUpload
              run: ./gradlew :lib-simpleloginet:bintrayUpload -PapiKey=${{ secrets.BINTRAY_API_KEY }}
        ```
    2. "封装清空触发条件"
        此功能整体框架和伪代码如下：
        1. 获取VelocityTracker实例和定义阈值；
        2. 收集每一个MotionEvent事件；
        3. 记录事件序列初始和结尾的x坐标；
        4. 用2和3收集到的条件区分点击和滑动；
        5. 用2和3收集到的条件区分计算滑动速度是否满足触发清空的条件；
        6. 清空
        ```Kotlin
        //监听滑动速度，封装清空触发条件伪代码
        //1. 获取VelocityTracker实例和定义阈值；
        //获取android.view.VelocityTracker实例对象，用于监听滑动速度
        val velocityTracker: VelocityTracker = VelocityTracker.obtain()

        //定义滑动的速度阈值，用于识别确实是滑动，因为手指头点击的时候也可能带有些许滑动
        //这里直接使用android.view.ViewConfiguration#getScaledMinimumFlingVelocity提供给我们的阈值来定义
        val clearTouchVelocityMinSlop = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        //定义清空滑动距离的阈值
        //这里直接使用android.view.ViewConfiguration#scaledTouchSlop的两倍来定义
        val clearTouchSlop = ViewConfiguration.get(context).scaledTouchSlop * 2

        //省略一些代码

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            return event?.let {
                //2. 收集每一个MotionEvent
                //每一个MotionEvent都告知velocityTracker
                //获取屏幕滑动速度的方法就是收集一连串的MotionEvent事件，其中的三个关键数据x、y坐标和事件发生的时间戳是得知滑动速度的关键因素
                velocityTracker.addMovement(it)
                //省略一些代码
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        //3. 记录事件序列初始x坐标；
                        downEventX = it.x
                    }
                    //在UP和CANCEL事件的时候进行速度的计算
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        //3. 记录事件序列结尾x坐标；it.x
                        if (isClearMoveWhenUp(it.x, downEventX)) {
                            //6. 清空
                            setText("")
                        }
                    }
                }
                true
            }?: false
        }

        //省略一些代码

        //关键方法：根据速度和滑动距离判断是否触发清空操作
        private fun isClearMoveWhenUp(v0: Float, v1: Float): Boolean {
            //5. 用2和3收集到的条件区分计算滑动速度是否满足触发清空的条件；
            //判断x方向的速度是否超过了阈值clearTouchVelocityMinSlop
            fun isClearMoveWhenUp0(): Boolean {
                velocityTracker.computeCurrentVelocity(100, clearTouchVelocityMaxSlop.toFloat())
                val result = abs(velocityTracker.xVelocity) >= clearTouchVelocityMinSlop
                velocityTracker.clear()
                return result
            }

            //4. 用2和3收集到的条件区分点击或滑动；
            //判断滑动距离是否超过了阈值clearTouchSlop
            fun isClearMoveWhenUp1(v0: Float, v1: Float): Boolean = clearTouchSlop <= abs(v0 - v1)

            return isClearMoveWhenUp0() && isClearMoveWhenUp1(v0, v1)
        }

        ```
    3. "封装点击明/密文切换触发条件"
        此功能整体框架和伪代码如下：
        1. 定义点击的时间阈值；
        2. 限定能够点击的范围为右边的icon做占得位置，记录DOWN事件的时间戳和X、Y坐标；
        3. 比较DOWN/UP事件的时间戳差值判断是否是一个点击；
        4. 比较DOWN/UP事件的X、Y坐标差值判断是否是一个点击（限定能够点击的范围）；
        5. 修改明/密文状态icon的显示；
        6. 修改明/密文的显示；
        ```Kotlin
        //封装点击明/密文切换触发条件伪代码
        //1. 定义点击的时间最小阈值，超过阈值不触发点击；
        val tapTimeout = ViewConfiguration.getTapTimeout() * 2

        //记录触摸事件数据的实体类
        private data class TouchTripleVo(val touchEventTime: Long, val x: Float, val y: Float)

        //省略一些代码

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            return event?.let {
                //省略一些代码
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val downEvent = it
                        //只有密码框的[2]才会有值 0左1上2右3下
                        compoundDrawables[2]?.apply {
                            val width = this.bounds.width()
                            //2. 限定能够点击的范围为右边的icon做占得位置；
                            if (downEvent.x > measuredWidth - width) {
                                //2. 记录DOWN事件的时间戳和X、Y坐标；
                                //将Down事件的时间x、y坐标记录在一个实体类中
                                touchEventTriple = TouchTripleVo(downEvent.eventTime, downEvent.x, downEvent.y)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val upEvent = it
                        //3. 比较DOWN/UP事件的时间戳差值判断是否是一个点击；
                        val isClickTap0: Boolean = tapTimeout > upEvent.eventTime.minus(touchEventTriple?.touchEventTime ?: 0L)
                        //4. 比较DOWN/UP事件的X、Y坐标差值判断是否是一个点击；
                        val isClickTap1: Boolean = touchSlop >= abs(upEvent.x - touchEventTriple?.x ?: 0f)
                        val isClickTap2: Boolean = touchSlop >= abs(upEvent.y - touchEventTriple?.y ?: 0f)
                        if(isClickTap0 && isClickTap1 && isClickTap2) {
                            //5. 修改明/密文状态icon的显示；
                            //6. 修改明/密文的显示；
                            updateAndGetTextVisible()
                            //省略：通过接口回调通知别人自己的状态是明文还是密文
                        }
                    }
                }
                true
            }?: false
        }

        private fun updateAndGetTextVisible(): Boolean {
            if (布尔值表达式or方法：当前是否密文) {
                修改图标为明文状态
            } else {
                修改图标为密文状态
            }
            //真正
            return (布尔值表达式or方法：修改后当前是否密文).apply {
                if (this == true) {
                    inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
        }
        ```
    4. "控制tips提示的时机"
        虽然提供了滑动清空的功能，但是如果没有明显的提示用户也是不知道的，这里我们参考原生的提示功能自己封装一个PopupWindow提示，原生的提示功能是通过调用android.widget.TextView#setError触发的(EditText是TextView的子类)，感兴趣的娃可以去了解一哈，也是对PopupWindow的一个封装，代码比较简单，不多加注释了；

        主要的做两件事情：
            1. 展示tips提示；
            2. 在合适时机触发1；
        ```kotlin
        //1. 展示tips提示；
        private fun showClearTips() {
            val textView = 代码生成一个textview
            val popupWindow = PopupWindow(textView)
            //略:popupWindow参数配置
            val radius: Float = 获取圆角值
            val bgDrawable = ShapeDrawable(RoundRectShape(FloatArray(8) { radius }, null, null))
            //略:设置bgDrawable颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                textView.background = bgDrawable
            } else {
                textView.setBackgroundDrawable(bgDrawable)
            }
            //设置不可点击
            popupWindow.isTouchable = false
            postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    popupWindow.showAsDropDown(this, 0, 10, Gravity.BOTTOM)
                } else {
                    popupWindow.showAsDropDown(this, 0, 10)
                }
                val dismissRunnable = Runnable { popupWindow.dismiss() }
                popupWindow.setOnDismissListener { removeCallbacks(dismissRunnable) }
                //3500秒后自动消失
                postDelayed(dismissRunnable, 3000)
            }, 500)
        }

        //2. 在合适时机触发1；
        override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
            if (focused && !isLastLaunchExist) {
                isLastLaunchExist = true
                //显示popupwindow 提示滑动清空
                showClearTips()
            }
        }
        ```
    5. "控件属性参数化(TypedArray)"
        定义attrs.xml文件，在构造函数中解析这些属性变成相应的参数即可，解析过程都是一些模板代码这里就不再赘述了，仅贴出attrs.xml文件源码如下：
        ```XML
        <resources>
            <declare-styleable name="SimpleLoginEt">
                <!--清空提示的文字，注意需要使用R.string.xxx引用-->
                <attr name="clear_tips" format="reference" />
                <!--清空提示框的背景色-->
                <attr name="clear_tips_bg_color" format="color" />
                <!--清空提示框的圆角值-->
                <attr name="clear_tips_bg_radius" format="dimension" />
                <!--密码模式下的右边icon的明文状态，注意需要使用R.drawable.xxx引用-->
                <attr name="icon_end_open" format="reference" />
                <!--密码模式下的右边icon的密文状态，注意需要使用R.drawable.xxx引用-->
                <attr name="icon_end_close" format="reference" />
            </declare-styleable>
        </resources>
        ```
    6. "其他细节"
        1. "版本适配，AppCompat"
            EditText如果有Android版本适配要求需要使用AppCompatEditText作为基类的话，我们拷贝一份SimpleLoginEt文件，然后将
            `class SimpleLoginEt : EditText`
            改为
            `class AppCompatSimpleLoginEt : AppCompatEditText`
            即可。
        2. "禁用setError功能"
            由于此功能的触发会导致drawableRight的icon被修改，因此我们需要禁用此功能；
            ```Kotlin
            override fun setError(error: CharSequence?, icon: Drawable?) {
                //禁用此功能，因为会影响设置drawable，感兴趣的同学也可以自定义自己的错误提示
                //super.setError(error, icon)
            }
            ```
        3. "首次上传发布到jcenter不能够立即在jcenter的maven库中找到，还需要申请发布一次"
            此处👇下边有一张图5：
            ![图5 Linked to jcenter](https://cdn.jsdelivr.net/gh/halohoop/cdn/github/20200514165322.png)
            申请发布首次之后，之后再提交新的发布版本就会自动发布到jcenter的maven库中，还不了解如何发布aar包到jcenter的朋友,可以参考本文结尾中相关阅读的文章。
4. 请一起来完善，欢迎给我提PR
    个人水平有限，欢迎读者勘误。
    项目基于开源协议[[Apache-2.0]](https://github.com/halohoop/SimpleLoginEditText/blob/master/LICENSE)托管在Github上，地址为[https://github.com/halohoop/SimpleLoginEditText](https://github.com/halohoop/SimpleLoginEditText)。
    1. 方向1：我们能看到AppCompatSimpleLoginEt和SimpleLoginEt这两个类的代码90%以上都是重复的，为了后期维护方便，不必维护两套代码，我们可以抽取出来，欢迎给我PR，一起来完善这个项目；
    2. 方向2：按照目前的实现左右的滑动都可以触发清空，可以区分一下将其中一个滑动去实现其他功能；
    3. 方向3：国际化，翻译，RTL适配与完善；
    4. 方向4：完善单元测试；
    5. Welcome more improvements;
5. 相关阅读
    1. [2020年最新使用gradle插件上传并发布jar包/aar包到jcenter方法]()
    2. [Dokka，为Kotlin+Java混合代码生成Javadoc文档，并打一个javadoc.jar包]()
    3. [用Gradle打一个源码sources.jar包]()