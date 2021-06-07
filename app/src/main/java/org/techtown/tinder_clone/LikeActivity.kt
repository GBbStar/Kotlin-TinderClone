package org.techtown.tinder_clone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.*

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

        userDB = Firebase.database.reference.child(DBKey.USERS)

        val currentUserDB = userDB.child(getCurrentUserID())
        currentUserDB.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child(DBKey.NANE).value == null){
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
        initSignOutButton()
        initMatchedListButton()
    }




    private fun initCardStackView(){
        val stackView = findViewById<CardStackView>(R.id.cardStackView)
        stackView.layoutManager = manager
        stackView.adapter = adapter

        manager.setStackFrom(StackFrom.Top)
        manager.setTranslationInterval(8.0f)
        manager.setSwipeThreshold(0.1f)
    }

    private fun initSignOutButton() {
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


    private fun initMatchedListButton() {
        val matchedListButton = findViewById<Button>(R.id.matchListButton)
        matchedListButton.setOnClickListener {
            startActivity(Intent(this, MatchedUserActivity::class.java))
        }
    }

    private fun getUnSelectedUsers() {
        userDB.addChildEventListener(object:ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child(DBKey.USER_ID).value != getCurrentUserID()
                    && snapshot.child(DBKey.LIKED_BY).child(DBKey.LIKE).hasChild(getCurrentUserID()).not()
                    && snapshot.child(DBKey.LIKED_BY).child(DBKey.DIS_LIKE).hasChild(getCurrentUserID()).not()){
                    val userId = snapshot.child(DBKey.USER_ID).value.toString()
                    var name = "undecided"
                    if (snapshot.child(DBKey.NANE).value != null){
                        name = snapshot.child(DBKey.NANE).value.toString()
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
                    it.name = snapshot.child(DBKey.NANE).value.toString()
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
        user[DBKey.USER_ID] = userId
        user[DBKey.NANE] = name
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
            .child(DBKey.LIKED_BY)
            .child(DBKey.LIKE)
            .child(getCurrentUserID())
            .setValue(true)

        saveMatchIfOtherUserLikedMe(card.userId)
    }

    private fun saveMatchIfOtherUserLikedMe(otherUserId: String) {
        val otherUserDB = userDB.child(getCurrentUserID()).child(DBKey.LIKED_BY).child(DBKey.LIKE).child(otherUserId)
        otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true){
                    userDB.child(getCurrentUserID())
                        .child(DBKey.LIKED_BY)
                        .child(DBKey.MATCH)
                        .child(otherUserId)
                        .setValue(true)
                    userDB.child(otherUserId)
                        .child(DBKey.LIKED_BY)
                        .child(DBKey.MATCH)
                        .child(getCurrentUserID())
                        .setValue(true)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun dislike() {
        val card = cardItems[manager.topPosition-1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child(DBKey.LIKED_BY)
            .child(DBKey.DIS_LIKE)
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