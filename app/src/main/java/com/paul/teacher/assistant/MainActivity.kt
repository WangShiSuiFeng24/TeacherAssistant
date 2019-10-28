package com.paul.teacher.assistant

import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.iflytek.cloud.*
import com.iflytek.cloud.ui.RecognizerDialog
import com.iflytek.cloud.ui.RecognizerDialogListener

import kotlinx.android.synthetic.main.activity_main.*
import com.iflytek.cloud.resource.Resource.setText
import org.json.JSONException
import org.json.JSONObject
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.paul.teacher.assistant.util.JsonParser
import java.util.LinkedHashMap

class MainActivity : AppCompatActivity() {

    var mIatDialog: RecognizerDialog? = null

    private val mIatResults = LinkedHashMap<String, String>()

    /**
     * 初始化监听器。
     */
    private val mInitListener = InitListener { code ->
        Log.d(this.localClassName, "SpeechRecognizer init() code = $code")
        if (code != ErrorCode.SUCCESS) {
            showTip("初始化失败，错误码：$code,请点击网址https://www.xfyun.cn/document/error-code查询解决方案")
        }
    }

    /**
     * 听写UI监听器
     */
    private val mRecognizerDialogListener = object : RecognizerDialogListener {
        override fun onResult(results: RecognizerResult, isLast: Boolean) {
            Log.d(this.javaClass.name, "recognizer result：" + results.resultString)

            /*
            if (mTranslateEnable) {
                printTransResult(results)
            } else {
                val text = JsonParser.parseIatResult(results.resultString)
                mResultText.append(text)
                mResultText.setSelection(mResultText.length())
            }*/

            val resultStr = updateResult(results);
            if (isLast) {
                Log.d(this.javaClass.name, "recognizer result：" + resultStr)
                showTip(resultStr);
            }
        }

        /**
         * 识别回调错误.
         */
        override fun onError(error: SpeechError) {
            //if (mTranslateEnable && error.errorCode == 14002) {
            //    showTip(error.getPlainDescription(true) + "\n请确认是否已开通翻译功能")
            //} else {
                showTip(error.getPlainDescription(true))
            //}
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        SpeechUtility.createUtility(this.applicationContext, SpeechConstant.APPID +"=5db04b35");

        fab.setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //    .setAction("Action", null).show()

            mIatDialog = RecognizerDialog(this, mInitListener)
            mIatDialog?.setListener(mRecognizerDialogListener)
            mIatDialog?.setParameter(SpeechConstant.VAD_EOS, "3000");
            mIatDialog?.show()

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 读取动态修正返回结果
    private fun updateResult(results: RecognizerResult): String {
        val text = JsonParser.parseIatResult(results.resultString)

        var sn: String? = null
        var pgs: String? = null
        var rg: String? = null
        // 读取json结果中的sn字段
        try {
            val resultJson = JSONObject(results.resultString)
            sn = resultJson.optString("sn")
            pgs = resultJson.optString("pgs")
            rg = resultJson.optString("rg")
        } catch (e: JSONException) {
            e.printStackTrace()
            return ""
        }

        //如果pgs是rpl就在已有的结果中删除掉要覆盖的sn部分
        if (pgs == "rpl") {
            Log.d(this.javaClass.name, "recognizer result replace：" + results.resultString)
            val strings = rg!!.replace("[", "").replace("]", "").split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val begin = Integer.parseInt(strings[0])
            val end = Integer.parseInt(strings[1])
            for (i in begin..end) {
                mIatResults.remove(i.toString() + "")
            }
        }

        mIatResults.put(sn!!, text)
        val resultBuffer = StringBuffer()
        for (key in mIatResults.keys) {
            resultBuffer.append(mIatResults.get(key))
        }

        return resultBuffer.toString()
        //mResultText.setText(resultBuffer.toString())
        //mResultText.setSelection(mResultText.length())
    }


    private fun showTip(str: String) {
        runOnUiThread {
            Snackbar.make(fab, str, Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null).show()
        }
    }

}
