package com.example.chattutorial

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.getstream.sdk.chat.viewmodel.ChannelHeaderViewModel
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel
import com.getstream.sdk.chat.viewmodel.bindView
import com.getstream.sdk.chat.viewmodel.factory.ChannelViewModelFactory
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel
import com.getstream.sdk.chat.viewmodel.messages.bindView
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.controllers.subscribeFor
import io.getstream.chat.android.client.events.TypingStartEvent
import io.getstream.chat.android.client.events.TypingStopEvent
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.name
import kotlinx.android.synthetic.main.activity_channel.channelHeaderView
import kotlinx.android.synthetic.main.activity_channel.messageInputView
import kotlinx.android.synthetic.main.activity_channel.messageListView
import kotlinx.android.synthetic.main.activity_channel_3.*

class ChannelActivity4 : AppCompatActivity(R.layout.activity_channel_4) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cid = checkNotNull(intent.getStringExtra(CID_KEY)) {
            "Specifying a channel id is required when starting ChannelActivity"
        }

        // step 1 - we create 3 separate ViewModels for the views so it's easy to customize one of the components
        val factory = ChannelViewModelFactory(cid)
        val channelHeaderViewModel: ChannelHeaderViewModel by viewModels { factory }
        val messageListViewModel: MessageListViewModel by viewModels { factory }
        val messageInputViewModel: MessageInputViewModel by viewModels { factory }

        // set custom AttachmentViewHolderFactory
        messageListView.setAttachmentViewHolderFactory(MyAttachmentViewHolderFactory())

        // step 2 = we bind the view and ViewModels. they are loosely coupled so its easy to customize
        channelHeaderViewModel.bindView(channelHeaderView, this)
        messageListViewModel.bindView(messageListView, this)
        messageInputViewModel.bindView(messageInputView, this)

        // step 3 - let the message input know when we open a thread
        messageListViewModel.mode.observe(this) {
            when (it) {
                is MessageListViewModel.Mode.Thread -> messageInputViewModel.setActiveThread(it.parentMessage)
                is MessageListViewModel.Mode.Normal -> messageInputViewModel.resetThread()
            }
        }

        // step 4 - handle navigate up state
        messageListViewModel.state.observe(this) {
            when (it) {
                is MessageListViewModel.State.NavigateUp -> finish()
            }
        }

        // step 5 - let the message input know when we are editing a message
        messageListView.setOnMessageEditHandler {
            messageInputViewModel.editMessage.postValue(it)
        }

        // step 6 - handle back button behaviour correctly when you're in a thread
        channelHeaderView.onBackClick = {
            messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
        }

        // make pressing back on hardware the same as channel header's back
        onBackPressedDispatcher.addCallback(this) {
            channelHeaderView.onBackClick()
        }

        // custom typing info header bar
        val nobodyTyping = "nobody is typing"

        typingHeader.text = nobodyTyping

        val currentlyTyping = mutableSetOf<String>()

        ChatClient
            .instance()
            .channel(cid)
            .subscribeFor(
                this,
                TypingStartEvent::class.java, TypingStopEvent::class.java
            ) { event ->
                when (event) {
                    is TypingStartEvent -> currentlyTyping.add(event.user.name)
                    is TypingStopEvent -> currentlyTyping.remove(event.user.name)
                }

                typingHeader.text = when {
                    currentlyTyping.isNotEmpty() -> currentlyTyping.joinToString(prefix = "typing: ")
                    else -> nobodyTyping
                }
            }
    }

    companion object {
        private const val CID_KEY = "key:cid"

        fun newIntent(context: Context, channel: Channel): Intent =
            Intent(context, ChannelActivity4::class.java).putExtra(CID_KEY, channel.cid)
    }
}
