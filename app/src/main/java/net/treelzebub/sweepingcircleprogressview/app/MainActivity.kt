package net.treelzebub.sweepingcircleprogressview.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import net.treelzebub.sweepingcircleprogressview.SweepingCircleProgressView

/**
 * Created by Tre Murillo on 12/21/16
 */
class MainActivity : Activity(), SweepingCircleProgressView.Listener {

    private val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val progress = findViewById(R.id.progress) as SweepingCircleProgressView
        progress.listen(this)

        val button = findViewById(R.id.button)
        button.setOnClickListener {
            progress.isIndeterminate = !progress.isIndeterminate
            progress.progress = if (progress.isIndeterminate) 0f else 100f
        }
    }

    override fun onModeChanged(isIndeterminate: Boolean) {
        val txt = "Indeterminate? $isIndeterminate"
        Log.i(TAG, txt); toast(txt)
    }

    override fun onAnimationReset() {
        val txt = "Animation Reset."
        Log.i(TAG, txt); toast(txt)
    }

    override fun onProgressUpdate(progress: Float) {
        Log.i(TAG, "Progress: $progress")
    }

    override fun onProgressEnd(progress: Float) {
        val txt = "Progress complete."
        Log.i(TAG, txt); toast(txt)
    }
}

private fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()