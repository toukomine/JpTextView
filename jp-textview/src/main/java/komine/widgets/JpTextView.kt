package komine.widgets

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.RequiresApi

/**
 * 日语假名标注TextView
 * @author komine
 */
class JpTextView(context: Context,attributeSet: AttributeSet?):View(context,attributeSet) {

    constructor(context: Context):this(context,null)

    /**
     * 源字符串
     */
    private var text:String = ""

    /**
     * 存放假名和索引的集合
     */
    private var mKanaList = mutableListOf<Kana>()

    private var mTextPaint:Paint = Paint()
    private var mKanaPaint:Paint = Paint()

    private val mSourceTextList = arrayListOf<String>()
    private var mTextColor:Int = Color.BLACK
    private var mKanaTextColor:Int = Color.BLACK
    private var mTextSize:Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,18f,Resources.getSystem().displayMetrics)
    private var mKanaTextSize:Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,12f,Resources.getSystem().displayMetrics)
    private var mOneMark:Boolean = false
    private var mBackgroundColor:Int = Color.WHITE

    private var kanaTextHeight = 0
    private var textHeight = 0
    init {
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.JpTextView)
        text = array.getString(R.styleable.JpTextView_sourceText) ?: ""
        mTextColor = array.getColor(R.styleable.JpTextView_sourceTextColor,mTextColor)
        mTextSize = array.getDimension(R.styleable.JpTextView_sourceTextSize,mTextSize)

        mKanaTextColor = array.getColor(R.styleable.JpTextView_kanaTextColor,mKanaTextColor)
        mKanaTextSize = array.getDimension(R.styleable.JpTextView_kanaTextSize,mKanaTextSize)
        mOneMark = array.getBoolean(R.styleable.JpTextView_oneMark,false)
        mBackgroundColor = array.getColor(R.styleable.JpTextView_bgColor,mBackgroundColor)

        array.recycle()

        mTextPaint.textSize = mTextSize
        mTextPaint.color = mTextColor
        mKanaPaint.textSize = mKanaTextSize
        mKanaPaint.color = mKanaTextColor

    }

    fun setKanaList(list: List<Kana>){
        mKanaList.clear()
        mKanaList.addAll(list)
        requestLayout()
    }

    fun setKanaList(keyMap:Map<String,String>){
        mKanaList.clear()
        for (item in keyMap) {
            val kana = Kana(item.key,item.value)
            mKanaList.add(kana)
        }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val parentWidthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val parentHeightSize = MeasureSpec.getSize(heightMeasureSpec)

        kanaTextHeight = mKanaPaint.fontMetricsInt.descent - mKanaPaint.fontMetricsInt.ascent
        textHeight = mTextPaint.fontMetricsInt.descent - mTextPaint.fontMetricsInt.ascent

        var width = parentWidthSize
        var height = parentHeightSize
        var maxWidth = 0
        var maxHeight = 0

        var needWrap = false
        val textWidth = mTextPaint.measureText(text).toInt()

        if(widthMode == MeasureSpec.AT_MOST){
            width = mTextPaint.measureText(text).toInt()
            maxWidth = parentWidthSize
            if(width > parentWidthSize){
                width = parentWidthSize
                needWrap = true
            }
        }else if(widthMode == MeasureSpec.EXACTLY){
            width = parentWidthSize
            maxWidth = width
            if(textWidth > parentWidthSize){
                needWrap = true
            }
        }

        if(heightMode == MeasureSpec.AT_MOST){
            val kanaDy = (mKanaPaint.fontMetricsInt.bottom - mKanaPaint.fontMetricsInt.top) / 2 - mKanaPaint.fontMetricsInt.bottom
            height = textHeight + kanaTextHeight + kanaDy
            maxHeight = Int.MAX_VALUE
        }else if(heightMode == MeasureSpec.EXACTLY){
            height = parentHeightSize
            maxHeight = height
        }

        if(needWrap){
            handleWrap(maxWidth,maxHeight)
            if(heightMode == MeasureSpec.AT_MOST){
                height = (height * (mSourceTextList.size) - kanaTextHeight)
            }
        }

        setMeasuredDimension(width,height)
    }

    private fun handleWrap(maxWidth:Int,maxHeight:Int){
        if(mSourceTextList.isNotEmpty()){
            return
        }

        val singleTextWidth = mTextPaint.measureText(text,0,1)
        val oneLineTextCount = maxWidth / singleTextWidth.toInt()
        val lineNumber = maxHeight / textHeight

        var currentIndex = 0
        var sumCount = text.length
        while (sumCount > 0){
            if(sumCount >= oneLineTextCount){
                mSourceTextList.add(text.substring(currentIndex,currentIndex + oneLineTextCount))
            }else{
                mSourceTextList.add(text.substring(currentIndex,currentIndex + sumCount))
            }
            if(mSourceTextList.size >= lineNumber){
                break
            }
            sumCount -= oneLineTextCount
            currentIndex += oneLineTextCount
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(mBackgroundColor)
        drawText(canvas)
    }

    private fun drawText(canvas:Canvas){
        val kanaDy = (mTextPaint.fontMetricsInt.bottom - mTextPaint.fontMetricsInt.top) / 2 - mTextPaint.fontMetricsInt.bottom

        if(mSourceTextList.isEmpty()){
            drawKanaText(canvas, text,(kanaTextHeight / 2f) + kanaDy)
            canvas.drawText(text,0f, (kanaTextHeight + textHeight).toFloat(),mTextPaint)
        }else{
            val lineHeight = kanaTextHeight + textHeight
            var offsetY = lineHeight.toFloat()
            for (text in mSourceTextList) {
                canvas.drawText(text,0f,offsetY,mTextPaint)
                drawKanaText(canvas,text,offsetY - (lineHeight / 2) - 10f)
                offsetY += lineHeight
            }
        }
    }

    private fun drawKanaText(canvas: Canvas,text: String,y:Float){
        var startIndex = 0
        for (kana in mKanaList){
            if(mOneMark && !kana.firstMark){
                continue
            }
            while (true)
            {
                val endIndex = findKeywordIndex(startIndex ,kana.text,text)
                if(endIndex == -1){
                    break
                }
                val left = mTextPaint.measureText(text,0,endIndex)
                canvas.drawText(kana.kana,left,y,mKanaPaint)
                kana.firstMark = false
                startIndex = endIndex + 1
            }
            startIndex = 0
        }
    }

    private fun findKeywordIndex(startIndex:Int,text: String,drawText:String? = this.text):Int{
        if(drawText == null){
            return -1
        }
        return drawText.indexOf(text,startIndex,false)
    }


    class Kana(val text: String, val kana:String){
        internal var firstMark:Boolean = true
    }
}