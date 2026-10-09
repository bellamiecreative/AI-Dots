package com.bellamiecreative.aidots

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var vibrator: Vibrator
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var statusText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var generationRunnable: Runnable? = null
    private var isGenerating = false
    private var generationToken = 0

    private val story = (
        "One quiet evening, a young traveler discovered a small lantern beside an old stone bridge. " +
        "Inside the lantern was a note: Even the smallest light can guide someone through the darkest night. " +
        "The traveler carried the lantern into the village and helped an elderly neighbor find the way home. " +
        "The next morning, the villagers placed lanterns along every street, and soon the whole village glowed. " +
        "The traveler understood the message: a small act of kindness can inspire many others. " +
        "From that day on, the village remembered that helping one person can brighten an entire world."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        buildInterface()
        addMessage(
            "Send any message to read an offline story. Vibration runs during the story. " +
                "Use the test button to check vibration immediately.",
            isUser = false
        )
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        val title = TextView(this).apply {
            text = "AI Dots"
            textSize = 22f
            setTextColor(Color.rgb(20, 20, 20))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 13f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(statusText, LinearLayout.LayoutParams(-1, -2))

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        chatContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }
        scrollView.addView(
            chatContainer,
            android.widget.FrameLayout.LayoutParams(-1, -2)
        )
        root.addView(
            scrollView,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        val testButton = Button(this).apply {
            text = "TEST VIBRATION NOW"
            isAllCaps = false
            setOnClickListener { testVibration() }
        }
        root.addView(testButton, LinearLayout.LayoutParams(-1, -2))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(4))
        }

        input = EditText(this).apply {
            hint = "Type a message..."
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            textSize = 16f
        }

        sendButton = Button(this).apply {
            text = "Send"
            isAllCaps = false
            setOnClickListener { submitMessage() }
        }

        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(sendButton, LinearLayout.LayoutParams(-2, -2))
        root.addView(composer, LinearLayout.LayoutParams(-1, -2))

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitMessage()
                true
            } else {
                false
            }
        }

        setContentView(root)
    }

    private fun testVibration() {
        try {
            if (!vibrator.hasVibrator()) {
                statusText.text = "This device reports no vibrator"
                addMessage("Vibration hardware is not available on this device.", false)
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(350)
            }

            statusText.text = "Vibration test requested"
        } catch (error: Exception) {
            statusText.text = "Vibration failed: ${error.javaClass.simpleName}"
        }
    }

    private fun submitMessage() {
        if (isGenerating) return

        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        addMessage(message, isUser = true)
        input.text.clear()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)

        val responseView = addMessage("", isUser = false)
        startStoryGeneration(responseView)
    }

    private fun startStoryGeneration(responseView: TextView) {
        isGenerating = true
        generationToken += 1
        val token = generationToken

        sendButton.isEnabled = false
        input.isEnabled = false
        statusText.text = "Generating story — vibration active"

        startRepeatingVibration()

        val characters = story.toCharArray()
        var index = 0

        val task = object : Runnable {
            override fun run() {
                if (token != generationToken || !isGenerating) return

                if (index >= characters.size) {
                    stopGeneration(responseView)
                    return
                }

                responseView.append(characters[index].toString())
                index++

                scrollToBottom()

                val lastChar = characters[index - 1]
                val delay = if (lastChar == '.' || lastChar == ',' || lastChar == ':') 100L else 22L
                handler.postDelayed(this, delay)
            }
        }

        generationRunnable = task
        handler.post(task)
    }

    private fun startRepeatingVibration() {
        if (!vibrator.hasVibrator()) {
            statusText.text = "Generating story — no vibrator detected"
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 900)
                val effect = VibrationEffect.createWaveform(timings, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 900), 0)
            }
        } catch (error: Exception) {
            statusText.text = "Story running; vibration unavailable"
        }
    }

    private fun stopGeneration(responseView: TextView) {
        generationRunnable?.let { handler.removeCallbacks(it) }
        generationRunnable = null
        stopVibration()

        isGenerating = false
        sendButton.isEnabled = true
        input.isEnabled = true
        statusText.text = "Story complete — vibration stopped"
        input.requestFocus()
        scrollToBottom()
    }

    private fun stopVibration() {
        try {
            vibrator.cancel()
        } catch (_: Exception) {
        }
    }

    private fun addMessage(text: String, isUser: Boolean): TextView {
        val message = TextView(this).apply {
            this.text = text
            textSize = 17f
            setTextColor(Color.rgb(25, 25, 25))
            setLineSpacing(dp(3).toFloat(), 1.15f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            if (isUser) {
                setBackgroundColor(Color.rgb(242, 242, 242))
                gravity = Gravity.START
            } else {
                gravity = Gravity.START
            }
        }

        val params = LinearLayout.LayoutParams(
            if (isUser) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
            if (isUser) {
                gravity = Gravity.END
                leftMargin = dp(48)
            }
        }

        chatContainer.addView(message, params)
        scrollToBottom()
        return message
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onStop() {
        super.onStop()
        if (isGenerating) {
            generationToken++
            generationRunnable?.let { handler.removeCallbacks(it) }
            generationRunnable = null
            isGenerating = false
            stopVibration()
            sendButton.isEnabled = true
            input.isEnabled = true
            statusText.text = "Generation stopped"
        }
    }

    override fun onDestroy() {
        generationToken++
        generationRunnable?.let { handler.removeCallbacks(it) }
        generationRunnable = null
        stopVibration()
        super.onDestroy()
    }
}
