package com.hyungilee.coroutinejob

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class MainActivity : AppCompatActivity() {

    private val PROGRESS_MAX = 100
    private val PROGRESS_START = 0
    private val JOB_TIME = 4000 //ms
    private lateinit var job: CompletableJob

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        job_button.setOnClickListener {
            if(!::job.isInitialized){
                initJob()
            }
            job_progress_bar.startJobOrCancel(job)
        }
    }

    private fun ProgressBar.startJobOrCancel(job: Job){
        // 확장함수 사용하기 (Kotlin 기능)
        // ProgressBar를 {}안에서 this로 접근할 수 있다.
        if(this.progress > 0){
            // = if(job.isActive)
            println("${job} is already active. Cancelling...")
            resetJob()
        }else{
            job_button.text = "Cancel job #1"
            CoroutineScope(IO + job).launch {
                //executed on a background thread.
                println("coroutine ${this} is activated with job ${job}")

                for(i in PROGRESS_START .. PROGRESS_MAX){
                    delay((JOB_TIME / PROGRESS_MAX).toLong())
                    this@startJobOrCancel.progress = i
                }
                //error(should put on the main thread(UI thread))
//                job_complete_text.text = "Job is complete"
                updateJobCompleteTextView("Job is complete")
            }

            // 현재 background thread 에서 실행중인 job이 모두 종료된다.
            // 따라서 고유의 IO job thread 를 할당하여 관리하는 것이 중요하다.
            //scope.cancel()
            // not interfered from other jobs.
//            CoroutineScope(IO + job2).launch {
//                //executed on a background thread.
//
//            }
        }
    }

    private fun resetJob() {
        if(job.isActive || job.isCompleted){
            job.cancel(CancellationException("Resetting job"))
        }
        // after cancelling the job, it can be re-use the job.
        initJob()
    }

    fun initJob(){
        job_button.text = "Start Job #1"
        updateJobCompleteTextView("")
        job = Job()
        //it will be executed whether the job is cancelled or completed.
        job.invokeOnCompletion {
            it?.message.let {
                var msg = it
                if(msg.isNullOrBlank()){
                    msg = "Unknown cancellation error."
                }
                println("${job}was cancelled. Reason: $msg")
                showToast(msg)
            }
        }
        job_progress_bar.max = PROGRESS_MAX
        job_progress_bar.progress = PROGRESS_START
    }

    private fun updateJobCompleteTextView(text: String){

        GlobalScope.launch(Main) {
            job_complete_text.text = text
        }
    }

    private fun showToast(text: String){
        // I can call and show the toast message anywhere i want to show.(Main Thread)
        GlobalScope.launch(Main) {
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
        }
    }
}
