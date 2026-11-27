package com.example.lovewidget

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoveScreen()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoveScreen() {
    val context = LocalContext.current
    val dbRef = remember { Firebase.database.reference.child("love_room") }

    var selectedEmoji by remember { mutableStateOf("❤️") }
    var noteText by remember { mutableStateOf("Скучаю!") }
    var statusMessage by remember { mutableStateOf("Готово к отправке") }

    val emojis = listOf("❤️", "😘", "🥺", "🔥", "💤", "🍕")

    LaunchedEffect(Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remoteEmoji = snapshot.child("emoji").getValue(String::class.java)
                val remoteNote = snapshot.child("note").getValue(String::class.java)

                if (remoteEmoji != null || remoteNote != null) {
                    val prefs = context.getSharedPreferences("love_widget_prefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        if (remoteEmoji != null) putString("last_emoji", remoteEmoji)
                        if (remoteNote != null) putString("last_note", remoteNote)
                        apply()
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        LoveWidget().updateAll(context)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                statusMessage = "Ошибка базы: ${error.message}"
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = selectedEmoji, fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            emojis.forEach { item ->
                Button(onClick = { selectedEmoji = item }) {
                    Text(text = item, fontSize = 22.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Короткая записка") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    val payload = mapOf(
                        "emoji" to selectedEmoji,
                        "note" to noteText.ifBlank { "❤️" },
                        "updatedAt" to System.currentTimeMillis()
                    )
                    dbRef.setValue(payload)
                        .addOnSuccessListener { statusMessage = "Отправлено!" }
                        .addOnFailureListener { statusMessage = "Не ушло: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отправить на виджет")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = statusMessage, fontSize = 14.sp)
    }
}
