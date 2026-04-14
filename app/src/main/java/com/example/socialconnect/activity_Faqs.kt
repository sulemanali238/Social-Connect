package com.example.socialconnect

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class activity_Faqs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faqs)

        // 1. Setup Back Button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 2. Setup FAQ Items
        // Format: setupFaq(ParentLayoutID, AnswerTextViewID, ArrowImageViewID)
        setupFaq(R.id.faqItem1, R.id.answerFaq1, R.id.arrowFaq1)
        setupFaq(R.id.faqItem2, R.id.answerFaq2, R.id.arrowFaq2)
        setupFaq(R.id.faqItem3, R.id.answerFaq3, R.id.arrowFaq3)
        setupFaq(R.id.faqItem4, R.id.answerFaq4, R.id.arrowFaq4)
        setupFaq(R.id.faqItem5, R.id.answerFaq5, R.id.arrowFaq5)
        setupFaq(R.id.faqItem6, R.id.answerFaq6, R.id.arrowFaq6)
        setupFaq(R.id.faqItem7, R.id.answerFaq7, R.id.arrowFaq7)
        setupFaq(R.id.faqItem8, R.id.answerFaq8, R.id.arrowFaq8)
    }

    /**
     * Logic for expanding/collapsing FAQ items
     */
    private fun setupFaq(parentId: Int, answerId: Int, arrowId: Int) {
        val parent = findViewById<LinearLayout>(parentId)
        val answer = findViewById<TextView>(answerId)
        val arrow = findViewById<ImageView>(arrowId)

        parent.setOnClickListener {
            if (answer.visibility == View.GONE) {
                // Expand
                answer.visibility = View.VISIBLE
                arrow.rotation = 90f // Rotate arrow to point down
            } else {
                // Collapse
                answer.visibility = View.GONE
                arrow.rotation = 0f // Rotate arrow back to right
            }
        }
    }
}