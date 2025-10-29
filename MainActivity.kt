package com.example.caixapos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase is automatic if google-services.json is present and plugin applied.
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CaixaApp()
                }
            }
        }
    }
}

data class OrderUi(
    val id: String = "",
    val itemName: String = "",
    val quantity: Int = 1,
    val priceCents: Long = 0,
    val paymentMethod: String = "Dinheiro",
    val paid: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun CaixaApp() {
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    val coroutine = rememberCoroutineScope()

    var orders by remember { mutableStateOf<List<OrderUi>>(emptyList()) }
    var itemName by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("Dinheiro") }

    val paymentOptions = listOf("Dinheiro", "Cartão - Crédito", "Cartão - Débito", "PIX", "Vale")

    LaunchedEffect(Unit) {
        // simple listener: fetch once (for real app use snapshot listeners)
        val snapshot = firestore.collection("orders").orderBy("timestamp").get().await()
        orders = snapshot.documents.map { doc ->
            OrderUi(
                id = doc.id,
                itemName = doc.getString("itemName") ?: "",
                quantity = (doc.getLong("quantity") ?: 1L).toInt(),
                priceCents = doc.getLong("priceCents") ?: 0L,
                paymentMethod = doc.getString("paymentMethod") ?: "Dinheiro",
                paid = doc.getBoolean("paid") ?: false,
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }.reversed()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("CaixaPOS — com Firebase", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = qtyText, onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } }, label = { Text("Quantidade") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter { ch -> ch.isDigit() } }, label = { Text("Preço (centavos)") }, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pagamento:")
            Spacer(Modifier.width(8.dp))
            DropdownMenuExample(selected = selectedPayment, options = paymentOptions) { selectedPayment = it }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val qty = qtyText.toIntOrNull() ?: 1
            val price = priceText.toLongOrNull() ?: 0L
            if (itemName.isNotBlank()) {
                coroutine.launch {
                    val data = hashMapOf(
                        "itemName" to itemName,
                        "quantity" to qty,
                        "priceCents" to price,
                        "paymentMethod" to selectedPayment,
                        "paid" to false,
                        "timestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("orders").add(data).await()
                    // naive refresh
                    val s = firestore.collection("orders").orderBy("timestamp").get().await()
                    orders = s.documents.map { doc ->
                        OrderUi(
                            id = doc.id,
                            itemName = doc.getString("itemName") ?: "",
                            quantity = (doc.getLong("quantity") ?: 1L).toInt(),
                            priceCents = doc.getLong("priceCents") ?: 0L,
                            paymentMethod = doc.getString("paymentMethod") ?: "Dinheiro",
                            paid = doc.getBoolean("paid") ?: false,
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }.reversed()
                    itemName = ""; qtyText = "1"; priceText = ""
                }
            }
        }, modifier = Modifier.align(Alignment.End)) {
            Text("Adicionar Pedido (nuvem)")
        }

        Spacer(Modifier.height(12.dp))
        Text("Pedidos", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(orders) { order ->
                OrderRow(order = order, onTogglePaid = {
                    coroutine.launch {
                        firestore.collection("orders").document(order.id).update("paid", !(order.paid)).await()
                    }
                }, onDelete = {
                    coroutine.launch {
                        firestore.collection("orders").document(order.id).delete().await()
                    }
                })
            }
        }

        Spacer(Modifier.height(12.dp))
        SummaryBar(orders = orders)
    }
}

@Composable
fun DropdownMenuExample(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selected) }
        androidx.compose.material.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt -> androidx.compose.material.DropdownMenuItem(onClick = { onSelected(opt); expanded = false }) { Text(opt) } }
        }
    }
}

@Composable
fun OrderRow(order: OrderUi, onDelete: () -> Unit, onTogglePaid: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = "${'$'}{order.itemName} x${'$'}{order.quantity}")
                val price = order.priceCents
                Text(text = "${'$'}{order.paymentMethod} • R$ ${'$'}{price / 100}.${'$'}{(price % 100).toString().padStart(2,'0')}")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (order.paid) "PAGO" else "ABERTO")
                Spacer(Modifier.height(8.dp))
                Row {
                    IconButton(onClick = onTogglePaid) { Text(if (order.paid) "Desmarcar" else "Marcar Pago") }
                    IconButton(onClick = onDelete) { Text("Excluir") }
                }
            }
        }
    }
}

@Composable
fun SummaryBar(orders: List<OrderUi>) {
    val totalCents = orders.sumOf { it.priceCents * it.quantity }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Total: R$ ${'$'}{totalCents / 100}.${'$'}{(totalCents % 100).toString().padStart(2, '0')}")
        // placeholder: no clear button for cloud example
    }
}
