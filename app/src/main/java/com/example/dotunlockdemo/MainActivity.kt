package com.example.dotunlockdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {
    //对象创建的顺序:构造方法 -> init代码块、属性的创建 -> onCreat(setContentView(R.layout.activity_main))
    //错误：private val  dots = arrayOf(dot1,dot2,dot3,dot4,dot5,dot6,dot7,dot8,dot9)

    //用数组保存9个圆点的对象 用于滑动过程中进行遍历
    //使用懒加载获取屏幕的状态栏和标题栏的高度,因为只在需要用时加载一次，之后再用就不需要再加载了
    private val barHeight:Int by lazy {
        //获取屏幕的尺寸
        val display = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(display)
        //获取绘制区域的尺寸
        val drawingRect = Rect()
        window.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT).getDrawingRect(drawingRect)

        display.heightPixels - drawingRect.height()
    }
    private val dots:Array<ImageView> by lazy {
        return@lazy arrayOf(sDot1,sDot2,sDot3,sDot4,sDot5,sDot6,sDot7,sDot8,sDot9)
    }

    //保存被点亮的视图
    private val allSelectViews = mutableListOf<ImageView>()

    //用于记录滑动过程中的轨迹
    private val password = StringBuilder()

    //记录原始密码
    private var orgPassword:String? = null

    //记录第一次设置的密码
    private var firstPassword:String? =null
    //保存所有线的tag值
    private val allLineTags = arrayOf(
        12,23,45,56,78,89,/*6条横线*/
        14,25,36,47,58,69,/*6条竖线*/
        24,35,57,68,15,26,48,59,
        16,34,49,67,18,27,29,38,/*斜线*/
        13,46,79,17,28,39,19,37/*跨一个点的线*/
    )

    //记录最后被点亮的圆点对象
    private var lastSelectedView:ImageView? = null

    //图片/视频请求码
    private  val REQUEST_IMAGE_CODE = 123
    private  val REQUEST_VIDEO_CODE = 124

    private var psd  = 123456
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //获取密码
        SharePreferenceUtil.getInstance(this).getPassword().also {
            if (it == null){
                mAlert.text = "请设置密码："
            }else{
                mAlert.text = "请输入密码："
                orgPassword = it
            }
        }

        //给图像添加点击事件
        mHeader.setOnClickListener {
            //从相册/相机获取一张图片
            Intent().apply {
                action = Intent.ACTION_PICK
                type = "image/*"
                startActivityForResult(this,REQUEST_IMAGE_CODE)
            }
        }

        //获取头像
        File(filesDir,"header.jpg").also {
            if (it.exists()){
                BitmapFactory.decodeFile(it.path).also { image ->
                    mHeader.setImageBitmap(image)
                }
            }
        }

        //重设密码
        resetBtn.setOnClickListener {
            firstPassword = null
            orgPassword = null
            mAlert.text = "请设置密码："
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_IMAGE_CODE -> {
                //图片 InputStream OutputStream Writer Reader
                //判断用户是否取消操作
                if (resultCode != Activity.RESULT_CANCELED){
                    //获取图片
                    val uri = data?.data

                    uri?.let {
                        contentResolver.openInputStream(uri).use {
                            //Bitmap
                            BitmapFactory.decodeStream(it).also { image ->
                                //显示图片
                                mHeader.setImageBitmap(image)
                                //把图片缓存起来 100 70
                                val file = File(filesDir,"header.jpg")
                                FileOutputStream(file).also {fos ->
                                    //将图片缓存到fos对应的路径中
                                    image.compress(Bitmap.CompressFormat.JPEG,50,fos)
                                }
                            }
                        }
                    }
                }
            }

            REQUEST_VIDEO_CODE -> {
                //视频
            }
        }
    }

    //监听触摸事件
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        //将触摸点的坐标转化到mContainer上
        val location = convertTouchLocationContainer(event!!)
        
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                findViewContainersPoint(location).also {
                    highlightView(it)
                }
            }
            MotionEvent.ACTION_MOVE ->{
                findViewContainersPoint(location).also {
                    highlightView(it)
                }
            }
            MotionEvent.ACTION_UP ->{
                if (ifInDots(location,dots)){
                    judgePassword()
                    reset()
                }else{
                    if (lastSelectedView != null) {
                        judgePassword()
                        reset()
                    }
                    return true
                }
            }
        }
        return true
    }

    //判定密码
    private fun judgePassword(){
        //判断shared_prefs中的密码是否存在
        if (orgPassword == null) {
            //是不是第一次设置密码
            if (firstPassword == null){
                //记录第一次的密码
                firstPassword = password.toString()
                //提示输入第二次
                mAlert.text = "请再次输入密码："
            }else{
                //判断两次密码是否相同
                if (firstPassword!! == password.toString()) {
                    //两次密码一致
                    mAlert.text = "密码设置成功，已解锁！"
                    //保存密码
                    psd = firstPassword!!.toInt()
                    SharePreferenceUtil.getInstance(this).savePassword(firstPassword!!)
                    Intent(this,MainActivity2::class.java).apply{
                        putExtra("psd",psd)
                        startActivityForResult(this,456)
                    }
                }else{
                    mAlert.text = "两次密码输入不一致，请重新设置！"
                    firstPassword = null
                }
            }
        }else{
            //判断两次密码是否相同
            if (orgPassword!! == password.toString()) {
                //两次密码一致
                mAlert.text = "解锁成功！"
                //保存密码
                psd = orgPassword!!.toInt()
                SharePreferenceUtil.getInstance(this).savePassword(orgPassword!!)
                Intent(this,MainActivity2::class.java).apply{
                    putExtra("psd",psd)
                    startActivity(this)
                }
            }else{
                mAlert.text = "解锁失败！"
                firstPassword = null
            }
        }
    }

    //判断触摸点是否在圆点上
    private fun ifInDots(point: Point,dotViews: Array<ImageView>):Boolean{
        for (dotView in dots) {
            getRectForView(dotView).also {
                if (it.contains(point.x,point.y)){
                    //触摸点在圆点
                    return true
                }
            }
        }
        //触摸点不在圆点
        return false
    }

    //点亮视图
    private fun highlightView(v:ImageView?){
        if (v != null && v.visibility == View.INVISIBLE) {
            //判断这个点是不是第一个点
            if (lastSelectedView == null) {
                //第一个点，只需要点亮 并且保存
                hightlightDot(v)
            }else {
                //在滑动的时候已经点亮过其他的点了
                //获取上一个点和这个点的线的tag值
                val previous:Int = (lastSelectedView?.tag as String).toInt()
                val current:Int = (v.tag as String).toInt()
                //Log.v("wac","current")
                val lineTag:Int = if (previous > current) current*10+previous else previous*10+current

                //判断是否有这条线
                if (allLineTags.contains(lineTag)) {
                    //点亮这个点
                    hightlightDot(v)
                    //点亮线
                    when(lineTag) {
                        //1.需要点亮两条线
                        13 -> {
                            mContainer.findViewWithTag<ImageView>(12.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(23.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        46 -> {
                            mContainer.findViewWithTag<ImageView>(45.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(56.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        79 -> {
                            mContainer.findViewWithTag<ImageView>(78.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(89.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        17 -> {
                            mContainer.findViewWithTag<ImageView>(14.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(47.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        28 -> {
                            mContainer.findViewWithTag<ImageView>(25.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(58.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        39 -> {
                            mContainer.findViewWithTag<ImageView>(36.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(69.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        19 -> {
                            mContainer.findViewWithTag<ImageView>(15.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(59.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        37 -> {
                            mContainer.findViewWithTag<ImageView>(35.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                            mContainer.findViewWithTag<ImageView>(57.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                        //2.只需点亮一条线
                        else -> {
                            mContainer.findViewWithTag<ImageView>(lineTag.toString()).apply {
                                visibility = View.VISIBLE
                                allSelectViews.add(this)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hightlightDot(v:ImageView) {
        //点亮这个点
        if (v.visibility ==View.INVISIBLE) {
            v.visibility = View.VISIBLE
            allSelectViews.add(v)
            password.append(v.tag)
            //当前点亮的点就是下一个点亮的上一个点
            lastSelectedView = v
        }
    }

    //还原操作
    private fun reset(){
        //遍历保存点亮点的数组
        for (item in allSelectViews) {
            item.visibility = View.INVISIBLE
        }

        //清空
        allSelectViews.clear()
        lastSelectedView = null

        Log.v("wac",password.toString())
        password.clear()
    }

    //将触摸点的坐标转化为相对容器的坐标
    private fun convertTouchLocationContainer(event: MotionEvent):Point{
        return Point().apply {
            x = (event.x - mContainer.x).toInt()
            y = (event.y - mContainer.y - barHeight).toInt()
        }
    }



    //获取当前这个触摸点所在的圆点视图
    private fun  findViewContainersPoint(point: Point):ImageView?{
        //遍历所有的点 是否包含这个point
        for (dotView in dots){
            //判断这个视图是否包含point
            getRectForView(dotView).also {
                if (it.contains(point.x,point.y)){
                    return dotView
                }
            }
        }
        return null
    }

    //获取视图对应的Rect
    private fun getRectForView(v:View) = Rect(v.left,v.top,v.right,v.bottom)
}