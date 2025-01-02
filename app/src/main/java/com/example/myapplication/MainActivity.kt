package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference.child("expenses")
    private val expenses = mutableListOf<Expense>()
    private lateinit var adapter: ExpenseAdapter
    private lateinit var totalView: TextView
    private var isOnline = true // Status inicial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar a Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val descriptionInput = findViewById<EditText>(R.id.descriptionInput)
        val amountInput = findViewById<EditText>(R.id.amountInput)
        val addButton = findViewById<Button>(R.id.addButton)
        val listView = findViewById<ListView>(R.id.listView)
        totalView = findViewById(R.id.totalView)

        // Configurando o adaptador personalizado
        adapter = ExpenseAdapter(this, expenses)
        listView.adapter = adapter

        // Monitorar conexão
        monitorConnection()

        // Carregar os dados existentes do Firebase
        loadExpenses()

        // Adicionar novo gasto ao Firebase
        addButton.setOnClickListener {
            val description = descriptionInput.text.toString().trim()
            val amountText = amountInput.text.toString().trim()

            if (description.isNotEmpty() && amountText.isNotEmpty()) {
                try {
                    val amount = amountText.toDouble()
                    saveExpense(description, amount)
                    descriptionInput.text.clear()
                    amountInput.text.clear()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Insira um valor numérico válido!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveExpense(description: String, amount: Double) {
        val expense = mapOf("description" to description, "amount" to amount)

        // Salvar no Firebase
        db.push().setValue(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Gasto salvo!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar gasto. Será sincronizado ao voltar a conexão.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadExpenses() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenses.clear()
                snapshot.children.forEach { child ->
                    val description = child.child("description").getValue(String::class.java) ?: ""
                    val amount = child.child("amount").getValue(Double::class.java) ?: 0.0
                    val id = child.key
                    expenses.add(Expense(id, description, amount))
                }

                adapter.notifyDataSetChanged()
                updateTotal()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Erro ao carregar dados: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateTotal() {
        val total = expenses.sumOf { it.amount }
        totalView.text = "Total: R$ ${String.format("%.2f", total)}"
    }

    private fun monitorConnection() {
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isOnline = snapshot.getValue(Boolean::class.java) ?: false
                invalidateOptionsMenu() // Atualiza o menu da Toolbar
            }

            override fun onCancelled(error: DatabaseError) {
                // Log de erro
            }
        })
    }

    // Inflar o menu na Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Atualizar o ícone de status na Toolbar
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val statusItem = menu?.findItem(R.id.status_icon)
        if (isOnline) {
            statusItem?.setIcon(R.drawable.ic_online) // Ícone para status online
        } else {
            statusItem?.setIcon(R.drawable.ic_offline) // Ícone para status offline
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar exclusão")
            .setMessage("Deseja excluir este gasto?")
            .setPositiveButton("Sim") { _, _ ->
                expense.id?.let { db.child(it).removeValue() }
                Toast.makeText(this, "Gasto excluído.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    inner class ExpenseAdapter(
        private val context: AppCompatActivity,
        private val items: List<Expense>
    ) : ArrayAdapter<Expense>(context, R.layout.list_item, items) {

        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = convertView ?: layoutInflater.inflate(R.layout.list_item, parent, false)

            val expense = items[position]
            val descriptionView = view.findViewById<TextView>(R.id.itemDescription)
            val deleteButton = view.findViewById<ImageButton>(R.id.deleteButton)

            descriptionView.text = "${expense.description} - R$ ${String.format("%.2f", expense.amount)}"

            // Clique no botão de exclusão
            deleteButton.setOnClickListener {
                confirmDelete(expense)
            }

            return view
        }
    }

    data class Expense(
        val id: String? = null,
        val description: String = "",
        val amount: Double = 0.0
    )
}
