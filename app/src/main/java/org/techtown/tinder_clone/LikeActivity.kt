package org.techtown.tinder_clone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity(), CardStackListener {

    private var auth : FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB : DatabaseReference
    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()
    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child("Users")

        val currentUserDB = userDB.child(getCurrentUserID())
        currentUserDB.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child("name").value == null){
                    // 유저 정보 넣기
                    showNameInputPopup()
                }
                // 유저 정보 갱신
                getUnSelectedUsers()
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })

        initCardStackView()
    }

    private fun getUnSelectedUsers() {
        userDB.addChildEventListener(object:ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child("userId").value == getCurrentUserID()
                    && snapshot.child("likedBy").child("Like").hasChild(getCurrentUserID()).not()
                    && snapshot.child("likedBy").child("disLike").hasChild(getCurrentUserID())){
                    val userId = snapshot.child("userId").value.toString()
                    var name = "undecided"
                    if (snapshot.child("name").value != null){
                        name = snapshot.child("name").value.toString()
                    }

                    cardItems.add(CardItem(userId,name))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }


            }

            // 이름이 바뀜
            // like, unlike 바뀜
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                cardItems.find{
                    it.userId == snapshot.key
                }?.let{
                    it.name = snapshot.child("name").value.toString()
                }

                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun initCardStackView(){
        val stackView = findViewById<CardStackView>(R.id.cardStackView)
        stackView.layoutManager = manager
        stackView.adapter = adapter

    }


    private fun showNameInputPopup(){
        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("이름을 입력해주세요")
            .setView(editText)
            .setPositiveButton("저장"){_,_ ->
                if(editText.text.isEmpty()){
                    showNameInputPopup()
                } else{
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun getCurrentUserID():String{
        if (auth.currentUser == null){
            // 로그인이 안되어 있음
            finish()
        }
        return auth.currentUser?.uid.orEmpty()
    }
    
    private fun saveUserName(name:String){
        val userId = getCurrentUserID()
        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String,Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDB.updateChildren(user)
        
        // 유저정보 가져오기
        getUnSelectedUsers()
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {
    }

    override fun onCardSwiped(direction: Direction?) {
        when(direction){
            Direction.Right -> like()
            Direction.Left-> dislike()
            else ->{}
        }
    }

    private fun like() {
        val card = cardItems[manager.topPosition-1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child("likedBy")
            .child("like")
            .child(getCurrentUserID())
            .setValue(true)


    }

    private fun dislike() {
        val card = cardItems[manager.topPosition-1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child("likedBy")
            .child("dislike")
            .child(getCurrentUserID())
            .setValue(true)
    }

    override fun onCardRewound() {
    }

    override fun onCardCanceled() {
    }

    override fun onCardAppeared(view: View?, position: Int) {
    }

    override fun onCardDisappeared(view: View?, position: Int) {
    }


}