package ca.fuwafuwa.kaku.Windows.Views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.TextView

import ca.fuwafuwa.kaku.*
import ca.fuwafuwa.kaku.Ocr.BoxParams
import ca.fuwafuwa.kaku.R
import ca.fuwafuwa.kaku.Windows.*
import ca.fuwafuwa.kaku.Windows.Data.ISquareChar
import ca.fuwafuwa.kaku.Windows.Interfaces.ICopyText
import ca.fuwafuwa.kaku.Windows.Interfaces.IRecalculateKanjiViews
import ca.fuwafuwa.kaku.Windows.Interfaces.ISearchPerformer
import ca.fuwafuwa.kaku.Windows.Window

/**
 * Created by 0xbad1d3a5 on 5/5/2016.
 */
class KanjiCharacterView : TextView, GestureDetector.OnGestureListener, IRecalculateKanjiViews
{
    private lateinit var mContext: Context
    private lateinit var mGestureDetector: GestureDetector
    private lateinit var mWindowCoordinator: WindowCoordinator
    private lateinit var mSearchPerformer: ISearchPerformer
    private lateinit var mKanjiChoiceWindow: KanjiChoiceWindow
    private lateinit var mEditWindow: EditWindow
    private lateinit var mSquareChar: ISquareChar

    private var mCellSizePx: Int = 0
    private var mScrollStartEvent: MotionEvent? = null

    constructor(context: Context) : super(context)
    {
        Init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {
        Init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    {
        Init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    {
        Init(context)
    }

    private fun Init(context: Context)
    {
        mContext = context
        mGestureDetector = GestureDetector(mContext, this)
    }

    fun getSquareChar(): ISquareChar
    {
        return mSquareChar
    }

    fun setDependencies(windowCoordinator: WindowCoordinator, searchPerformer: ISearchPerformer)
    {
        mWindowCoordinator = windowCoordinator
        mSearchPerformer = searchPerformer

        mKanjiChoiceWindow = mWindowCoordinator.getWindowOfType(WINDOW_KANJI_CHOICE)
        mEditWindow = mWindowCoordinator.getWindowOfType(WINDOW_EDIT)
    }

    fun setText(squareChar: ISquareChar)
    {
        mSquareChar = squareChar
        text = squareChar.char
    }

    fun setCellSize(px: Int)
    {
        mCellSizePx = dpToPx(context, pxToDp(context, px) - 2)
    }

    fun highlight()
    {
        background = mContext.getDrawable(R.drawable.bg_translucent_border_0_blue_blue)
    }

    fun highlightLight()
    {
        background = mContext.getDrawable(R.drawable.bg_transparent_border_0_nil_default)
    }

    fun unhighlight()
    {
        background = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
    {
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20.toFloat())
        setTextColor(Color.BLACK)
        setMeasuredDimension(mCellSizePx, mCellSizePx)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean
    {
        mGestureDetector.onTouchEvent(e)

        if (e.action == MotionEvent.ACTION_UP)
        {
            visibility = View.VISIBLE

            if (mScrollStartEvent != null)
            {
                mScrollStartEvent = null

                val choiceResult = mKanjiChoiceWindow.onSquareScrollEnd(e)
                when (choiceResult.first)
                {
                    ChoiceResultType.SWAP ->
                    {
                        text = choiceResult.second
                        mSquareChar.text = choiceResult.second
                        recalculateKanjiViews()
                    }
                    ChoiceResultType.EDIT ->
                    {
                        val window = getProperWindow<Window>()
                        window.hide()

                        mEditWindow.setInfo(mSquareChar)
                        mEditWindow.setInputDoneCallback(this)
                        mEditWindow.show()
                    }
                    ChoiceResultType.DELETE ->
                    {
                        mSquareChar.text = ""
                        recalculateKanjiViews()
                    }
                    ChoiceResultType.NONE ->
                    {
                        // Do nothing
                    }
                }
            }
        }

        return true
    }

    override fun recalculateKanjiViews()
    {
        val cwindow = getProperWindow<IRecalculateKanjiViews>()
        cwindow.recalculateKanjiViews()

        val window = getProperWindow<Window>()
        window.show()
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean
    {
        highlightLight()
        mSquareChar.userTouched = true
        mSearchPerformer.performSearch(mSquareChar)
        return true
    }

    override fun onScroll(motionEvent: MotionEvent, motionEvent1: MotionEvent, v: Float, v1: Float): Boolean
    {
        // scroll event start
        if (mScrollStartEvent == null)
        {
            mScrollStartEvent = motionEvent

            unhighlight()

            visibility = View.INVISIBLE

            mKanjiChoiceWindow.onSquareScrollStart(mSquareChar, getKanjiBoxParams(), mSquareChar.displayData.instantMode)
        }
        // scroll event continuing
        else {
            mKanjiChoiceWindow.onSquareScroll(motionEvent1)
        }

        return true
    }

    override fun onDown(motionEvent: MotionEvent): Boolean
    {
        return false
    }

    override fun onFling(motionEvent: MotionEvent, motionEvent1: MotionEvent, v: Float, v1: Float): Boolean
    {
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent)
    {
        val window = getProperWindow<ICopyText>()
        window.copyText()
    }

    override fun onShowPress(e: MotionEvent?)
    {
    }

    private fun <WindowType> getProperWindow() : WindowType
    {
        return if (mSquareChar.displayData.instantMode)
        {
            mWindowCoordinator.getWindowOfType(WINDOW_INSTANT_KANJI)
        }
        else {
            mWindowCoordinator.getWindowOfType(WINDOW_INFO)
        }
    }

    private fun getKanjiBoxParams() : BoxParams
    {
        var pos = IntArray(2)
        getLocationOnScreen(pos)
        return BoxParams(pos[0], pos[1], width, height)
    }

    companion object
    {
        private val TAG = KanjiCharacterView::class.java.name
    }
}
