package com.example.buyzone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    val searchText = remember { mutableStateOf("") }
    val products = remember { mutableStateListOf<Product>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {

        uploadSampleProductsToFirestore(db) // DELETE this line after first successful upload

        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                products.clear()

                for (document in result) {
                    val product = document.toObject(Product::class.java)
                    products.add(product)
                }

                isLoading = false
            }
            .addOnFailureListener { error ->
                errorMessage = error.message ?: "Failed to load products"
                isLoading = false
            }
    }

    val categories = products
        .map { it.category }
        .distinct()
        .filter { it.isNotBlank() }

    val filteredProducts = products.filter {
        it.name.contains(searchText.value, ignoreCase = true) ||
                it.category.contains(searchText.value, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BuyZone",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
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
                    icon = {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("cart") },
                    icon = {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                    },
                    label = { Text("Cart") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("profile") },
                    icon = {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    },
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
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {

                    item {
                        Text(
                            text = "Find your best products",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = searchText.value,
                            onValueChange = { searchText.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search products") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            },
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        if (categories.isEmpty()) {
                            Text("No categories available")
                        } else {
                            LazyRow {
                                items(categories) { category ->
                                    CategoryItem(category = category)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Popular Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
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

fun uploadSampleProductsToFirestore(db: FirebaseFirestore) {

    val sampleProducts = listOf(
        Product("Wireless Headphones", "£49.99", "Electronics"),
        Product("Smart Watch", "£79.99", "Electronics"),
        Product("Men T-Shirt", "£19.99", "Fashion"),
        Product("Running Shoes", "£59.99", "Shoes"),
        Product("Skin Care Kit", "£24.99", "Beauty"),
        Product("Java Programming Book", "£29.99", "Books")
    )

    for (product in sampleProducts) {
        db.collection("products")
            .document(product.name)
            .set(product)
    }
}

@Composable
fun CategoryItem(category: String) {
    Box(
        modifier = Modifier
            .padding(end = 10.dp)
            .background(
                color = Color(0xFFFFD8B1),
                shape = RoundedCornerShape(16.dp)
            )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            color = Color(0xFFFFE7D1),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Img")
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = product.category,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = product.price,
                        color = Color(0xFFCC6A00),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onAddToCart
            ) {
                Text("Add")
            }
        }
    }
}