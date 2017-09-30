# ZXingLib
zxing二维码扫描库


How to use:

Step 1. Add the JitPack repository to your build file
		allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
Step 2. Add the dependency

	dependencies {
	        compile 'com.github.imgod1:ZXingLib:1.0.0'
	}
  
Step 3. use in code
  CaptureActivity.actionStartForResult(activity,requestCode)
  
  
  
  
  第一个步骤中的repositories需要注意
  buildscript {
    repositories {
        jcenter()
        // DO NOT ADD IT HERE!!!
    }
    ...
  }

  allprojects {
    repositories {
        mavenLocal()
        jcenter()
        // ADD IT HERE
        maven { url "https://jitpack.io" }
    }
  }
  
  
  
  
  /**
 * 解析二维码的界面
 * 如果扫描到了二维码 会返回给上个界面 key是此类的常量 RESULT_KEY
 * 如果没有扫描到二维码 会返回请求失败的code给上个界面 code为 RESULT_CODE_NOT_FIND_QR
 * 可以依赖下来之后自己修改界面什么的 使之更符合你自己app的主题
 * 注意点:
 * 因为这就是加一个二维码扫描lib而已 所以并没有对android 6.0+上的动态权限做适配
 * 需要用户在自己的项目中 去适配动态权限申请
 */
