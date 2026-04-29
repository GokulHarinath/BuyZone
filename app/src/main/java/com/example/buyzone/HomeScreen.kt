package com.example.buyzone

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

data class Product(
    val name: String = "",
    val price: String = "",
    val category: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val context = LocalContext.current

    var searchText by remember { mutableStateOf("") }
    val products = remember { mutableStateListOf<Product>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(context, "Photo captured successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("products")
            .get()
            .addOnSuccessListener { result ->
                products.clear()
                for (document in result) {
                    products.add(document.toObject(Product::class.java))
                }
                isLoading = false
            }
            .addOnFailureListener { error ->
                errorMessage = error.message ?: "Failed to load products"
                isLoading = false
            }
    }

    val categories = products.map { it.category }.distinct().filter { it.isNotBlank() }

    val filteredProducts = products.filter {
        it.name.contains(searchText, ignoreCase = true) ||
                it.category.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("BuyZone", fontWeight = FontWeight.Bold)
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .clickable {
                                val permissionCheck = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                )

                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                    )

                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable {
                                navController.navigate("cart")
                            }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { navController.navigate("home") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("cart") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Cart") },
                    label = { Text("Cart") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("profile") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFFF3E0),
                                    Color(0xFFFFE0B2)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {

                    item {
                        Text(
                            text = "Welcome to BuyZone",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Text(
                            text = "Search, browse and add your favourite products to cart.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search products or categories") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        if (categories.isEmpty()) {
                            Text("No categories available")
                        } else {
                            LazyRow {
                                items(categories) { category ->
                                    CategoryItem(category)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Available Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (filteredProducts.isEmpty()) {
                        item {
                            Text("No products found")
                        }
                    } else {
                        items(filteredProducts) { product ->
                            ProductCard(
                                product = product,
                                onViewDetails = {
                                    navController.navigate("productDetails")
                                },
                                onAddToCart = {
                                    CartManager.addToCart(product)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(category: String) {
    Box(
        modifier = Modifier
            .padding(end = 10.dp)
            .background(Color(0xFFFFCC80), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = category,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onViewDetails: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.category,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.price,
                color = Color(0xFFCC6A00),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAddToCart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Cart")
            }
        }
    }
}