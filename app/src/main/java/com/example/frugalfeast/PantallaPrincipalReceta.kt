package com.example.frugalfeast

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PantallaPrincipalReceta : AppCompatActivity() {

    private lateinit var imgReceta: ImageView
    private lateinit var nombreReceta: TextView
    private lateinit var tvTiempoReceta: TextView
    private lateinit var tvPorcionesReceta: TextView
    private lateinit var tvDificultadReceta: TextView
    private lateinit var preparacionReceta: TextView
    private lateinit var listaIngredientes: TextView
    private lateinit var btnAgregarReceta: Button

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_principal_receta)

        // Inicializar vistas
        imgReceta = findViewById(R.id.imgReceta)
        nombreReceta = findViewById(R.id.nombreReceta)
        tvTiempoReceta = findViewById(R.id.tv_tiempo_receta_dia)
        tvPorcionesReceta = findViewById(R.id.tv_porciones_receta_dia)
        tvDificultadReceta = findViewById(R.id.tv_dificultad_receta_dia)
        preparacionReceta = findViewById(R.id.preparacionReceta)
        listaIngredientes = findViewById(R.id.listaIngredientes)
        btnAgregarReceta = findViewById(R.id.btn_agregar_receta)

        auth = FirebaseAuth.getInstance()

        val recetaId = intent.getStringExtra("receta_id") ?: ""
        if (recetaId.isNotEmpty()) {
            cargarRecetaDesdeFirebase(recetaId)
            verificarRecetaFavorita(recetaId)
        }
    }

    private fun cargarRecetaDesdeFirebase(recetaId: String) {
        db.collection("Receta")
            .document(recetaId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: ""
                    val imagenUrl = document.getString("imagenUrl") ?: ""
                    val tiempo = document.getString("tiempo") ?: ""
                    val porciones = document.getString("porciones") ?: ""
                    val dificultad = document.getString("dificultad") ?: ""
                    val preparacion = document.getString("preparacion") ?: ""
                    val ingredientes = document.get("ingredientes") as? List<String> ?: listOf()

                    // Actualizar la UI
                    nombreReceta.text = nombre
                    tvTiempoReceta.text = "$tiempo horas"
                    tvPorcionesReceta.text = "$porciones porciones"
                    tvDificultadReceta.text = dificultad
                    preparacionReceta.text = preparacion

                    if (imagenUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(imagenUrl)
                            .placeholder(R.drawable.baseline_image_24)
                            .fitCenter()
                            .into(imgReceta)
                    }

                    val ingredientesTexto = ingredientes.joinToString("\n• ", "• ")
                    listaIngredientes.text = ingredientesTexto

                    // Acción del botón de favorito
                    btnAgregarReceta.setOnClickListener {
                        if (isFavorite) {
                            eliminarRecetaFavorita(recetaId)
                        } else {
                            guardarRecetaFavorita(
                                recetaId,
                                nombre,
                                imagenUrl,
                                tiempo,
                                dificultad,
                                preparacion,
                                ingredientes
                            )
                        }
                    }

                } else {
                    Toast.makeText(this, "Receta no encontrada", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar receta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verificarRecetaFavorita(recetaId: String) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid
            val docRef = db.collection("usuarios").document(userId)
                .collection("recetas_guardadas").document(recetaId)

            docRef.get()
                .addOnSuccessListener { document ->
                    isFavorite = document.exists()
                    btnAgregarReceta.text = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos"
                }
        }
    }

    private fun guardarRecetaFavorita(
        recetaId: String,
        nombre: String,
        imagenUrl: String,
        tiempo: String,
        dificultad: String,
        preparacion: String,
        ingredientes: List<String>
    ) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid
            val recetaMap = hashMapOf(
                "nombre" to nombre,
                "imagenUrl" to imagenUrl,
                "tiempo" to tiempo,
                "dificultad" to dificultad,
                "preparacion" to preparacion,
                "ingredientes" to ingredientes
            )
            db.collection("usuarios").document(userId)
                .collection("recetas_guardadas").document(recetaId)
                .set(recetaMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Receta guardada", Toast.LENGTH_SHORT).show()
                    isFavorite = true
                    btnAgregarReceta.text = "Quitar de favoritos"
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar receta", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun eliminarRecetaFavorita(recetaId: String) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid
            db.collection("usuarios").document(userId)
                .collection("recetas_guardadas").document(recetaId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Receta eliminada de favoritos", Toast.LENGTH_SHORT).show()
                    isFavorite = false
                    btnAgregarReceta.text = "Agregar a favoritos"
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al eliminar receta", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
