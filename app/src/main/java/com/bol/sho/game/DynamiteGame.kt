package com.bol.sho.game

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.bol.sho.R
import com.bol.sho.databinding.DynamiteGameBinding

class DynamiteGame : AppCompatActivity() {

    private lateinit var binding: DynamiteGameBinding

    private lateinit var rotateAnimation: ObjectAnimator

    private val elementsList = listOf(
        R.drawable.mega_el1,
        R.drawable.mega_el2,
        R.drawable.mega_el3,
        R.drawable.mega_el4,
        R.drawable.mega_el5
    )
    private var resultList = mutableListOf<Int>()
    private var credit = 0
    private var bet = 10
    private var win = 0
    private var isRotate = false
    private var isHasCredits = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DynamiteGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        credit = intent.getIntExtra("credits", -1)

        binding.credits.text = credit.toString()
        binding.bet.text = bet.toString()
        binding.win.text = win.toString()

        binding.ivRotate.setOnClickListener {
            checkBet()
            if (isHasCredits) {
                binding.textWin.visibility = View.INVISIBLE
                isRotate = true
                resultList.clear()
                rotateAnim()
                it.isEnabled = false
                playAnim((2..5).random(), binding.image1, true)
                playAnim((6..9).random(), binding.image2, true)
                playAnim((10..15).random(), binding.image3, false)
            }
        }
    }
    private fun checkBet() {
        if ((credit - bet) >= 0) {
            credit -= bet
            binding.credits.text = credit.toString()
        } else {
            isHasCredits = false
            binding.textWin.visibility = View.VISIBLE
            binding.textWin.text = getString(R.string.lose_text)
            binding.ivRotate.isEnabled = false
        }
    }

    private fun winAnim(sum: Int) {
        ValueAnimator.ofInt(0, win + sum).apply {
            addUpdateListener {
                binding.win.text = it.animatedValue.toString()
            }
            doOnEnd {
                win = sum
                winTextAnim(sum)
            }
            duration = 1000
            start()
        }
    }

    private fun creditAnim(sum: Int) {
        val winCount = ValueAnimator.ofInt(sum, win - sum).apply {
            addUpdateListener {
                binding.win.text = it.animatedValue.toString()
            }
        }
        val balanceCount = ValueAnimator.ofInt(credit, credit + sum).apply {
            addUpdateListener {
                binding.credits.text = it.animatedValue.toString()
            }
        }
        AnimatorSet().apply {
            playTogether(winCount, balanceCount)
            duration = 1000
            doOnEnd {
                binding.ivRotate.isEnabled = true
                credit += sum
                win -= sum
            }
            start()
        }
    }

    private fun winTextAnim(sum: Int) {
        val anim = ObjectAnimator.ofFloat(binding.textWin, "alpha", 0f, 1f).apply {
            doOnEnd {
                creditAnim(sum)
            }
        }
        binding.textWin.visibility = View.VISIBLE
        anim.duration = 600
        anim.start()
    }

    private fun checkResult() {
        if (resultList.isEmpty()) return

        val el1 = resultList[0]
        val el2 = resultList[1]
        val el3 = resultList[2]

        val currentWinResult: Int

        if (el1 == el2 && el1 == el3) {
            currentWinResult = bet * 30
            winAnim(currentWinResult)
        } else {
            binding.ivRotate.isEnabled = true
        }
    }

    private fun rotateAnim() {
        rotateAnimation = ObjectAnimator.ofFloat(binding.ivRotate, View.ROTATION, 0f, 360f)
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE
        rotateAnimation.duration = 400
        rotateAnimation.interpolator = LinearInterpolator()
        rotateAnimation.start()
    }

    private fun playAnim(repeat: Int, image: ImageView, isRotate: Boolean): Animator {
        val start = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                val v = it.animatedValue as Float
                image.scaleY = (1f - v) / 1f
            }
            doOnEnd {
                val element = elementsList.shuffled().first()
                image.setImageResource(element)
                if (repeat == 1) {
                    resultList.add(element)
                }
            }
        }
        val finish= ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener {
                val v = it.animatedValue as Float
                image.scaleY = (1f - v) / 1f
            }
            doOnEnd {
            }
        }
        return AnimatorSet().apply {
            playSequentially(start, finish)
            duration = 100
            doOnEnd {
                if (repeat > 1) {
                    playAnim(repeat - 1, image, isRotate)
                } else if (repeat == 1 && !isRotate) {
                    rotateAnimation.cancel()
                    checkResult()
                }
            }
            start()
        }
    }
}