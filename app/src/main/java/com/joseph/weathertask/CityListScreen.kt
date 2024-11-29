package com.joseph.weathertask

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Card
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.joseph.weathertask.WeatherViewModel
import com.joseph.weathertask.api.NetworkResponse
import com.joseph.weathertask.api.WeatherModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

// Dados simulados para exemplo
data class Cidade(val nome: String, val temperatura: String)

// Tela principal
@Composable
fun CityListScreen(weatherViewModel: WeatherViewModel) {
    var novaCidade by remember { mutableStateOf("") }
    val cidades = remember { mutableStateListOf<Cidade>() }
    val weatherResult by weatherViewModel.weatherResult.observeAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var cidadeSelecionada by remember { mutableStateOf<Cidade?>(null) }

    // Variável para armazenar o primeiro valor fixo da cidade
    var cidadeFixaNome by remember { mutableStateOf<String?>(null) }
    var cidadeFixaTemperatura by remember { mutableStateOf<String?>(null) }
    var cidadeFixaClima by remember { mutableStateOf<String?>(null) }
    var weatherData: WeatherModel? by remember { mutableStateOf(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card de clima fixo para cidade inicial
        when(weatherResult){
            is NetworkResponse.Error -> {
                androidx.compose.material3.Text(text = "No Data")
            }
            is NetworkResponse.Success -> {
                weatherData = (weatherResult as NetworkResponse.Success<WeatherModel>).data

                // Apenas atualize o valor fixo se ele ainda estiver nulo
                if (cidadeFixaNome == null) {
                    cidadeFixaNome = weatherData?.location?.name
                    cidadeFixaTemperatura = weatherData?.current?.temp_c
                    cidadeFixaClima = weatherData?.current?.humidity
                }

                cidadeFixaNome?.let { nome ->
                    cidadeFixaTemperatura?.let { temperatura ->
                        cidadeFixaClima?.let { clima ->
                            CidadeFixaCard(cidadeNome = nome, temperatura = temperatura, clima = clima)
                        }
                    }
                }
            }
            NetworkResponse.Loading -> {
                androidx.compose.material3.CircularProgressIndicator()
            }
            null -> {
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto para adicionar novas cidades
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = novaCidade,
                onValueChange = { novaCidade = it },
                textStyle = TextStyle(fontSize = 18.sp)
            )
            Button(
                onClick = {
                    weatherViewModel.getData(novaCidade, "10")
                    keyboardController?.hide()
                }
            ) {
                Text("Adicionar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Observa o resultado da chamada para atualizar a lista de cidades
        when (weatherResult) {
            is NetworkResponse.Loading -> {
                Text("Carregando...")
            }
            is NetworkResponse.Success -> {
                weatherData = (weatherResult as NetworkResponse.Success<WeatherModel>).data
                val cidade = Cidade(novaCidade, "${weatherData?.current?.temp_c}ºC")
                if (!cidades.any { it.nome == cidade.nome } && novaCidade.isNotBlank()) {
                    if (cidade.nome != ""){
                        cidades.add(cidade)
                    }

                }
                novaCidade = ""
            }
            is NetworkResponse.Error -> {
                Text("Erro ao carregar dados do clima")
            }
            else -> {}
        }

        // Lista de cidades adicionadas
        cidades.forEach { cidade ->
            CidadeItem(
                cidade = cidade,
                onClick = { cidadeSelecionada = cidade } // Definir cidade selecionada
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Exibir popup se uma cidade estiver selecionada
        cidadeSelecionada?.let {
            weatherData?.let { data ->
                CidadeDetalhesDialog(
                    data = data,
                    onDismiss = { cidadeSelecionada = null }
                )
            }
        }
    }
}

// Função para o Card de cidade fixa
@Composable
fun CidadeFixaCard(cidadeNome: String, temperatura: String, clima: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = cidadeNome, style = MaterialTheme.typography.h5)
            Text(text = temperatura, style = MaterialTheme.typography.h6)
            Text(text = clima, style = MaterialTheme.typography.body2)
        }
    }
}

// Função para os itens da lista de cidades
@Composable
fun CidadeItem(cidade: Cidade, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = cidade.nome, style = MaterialTheme.typography.body1)
        Text(text = cidade.temperatura, style = MaterialTheme.typography.body1)
    }
}

// Tela de detalhes do clima
@Composable
fun CidadeDetalhesScreen(cidadeNome: String, weatherViewModel: WeatherViewModel = viewModel()) {
    LaunchedEffect(cidadeNome) {
        weatherViewModel.getData(cidadeNome, "1")
    }

    val weatherResult by weatherViewModel.weatherResult.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (weatherResult) {
            is NetworkResponse.Loading -> {
                Text("Carregando detalhes do clima...")
            }
            is NetworkResponse.Success -> {
                val weatherData = (weatherResult as NetworkResponse.Success<WeatherModel>).data

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location icon",
                            modifier = Modifier.size(40.dp)
                        )
                        androidx.compose.material3.Text(text = weatherData.location.name, fontSize = 30.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Text(
                            text = weatherData.location.country,
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Text(
                        text = " ${weatherData.current.temp_c} ° c",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    AsyncImage(
                        modifier = Modifier.size(160.dp),
                        model = "https:${weatherData.current.condition.icon}".replace("64x64","128x128"),
                        contentDescription = "Condition icon"
                    )
                    androidx.compose.material3.Text(
                        text = weatherData.current.condition.text,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                WeatherKeyVal("Humidity",weatherData.current.humidity)
                                WeatherKeyVal("Wind Speed",weatherData.current.wind_kph+" km/h")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                WeatherKeyVal("UV",weatherData.current.uv)
                                WeatherKeyVal("Participation",weatherData.current.precip_mm+" mm")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                WeatherKeyVal("Local Time",weatherData.location.localtime.split(" ")[1])
                                WeatherKeyVal("Local Date",weatherData.location.localtime.split(" ")[0])
                            }
                        }
                    }



                }
            }
            is NetworkResponse.Error -> {
                Text("Erro ao carregar detalhes do clima para $cidadeNome")
            }
            else -> {}
        }
    }
}

@Composable
fun CidadeDetalhesDialog(data : WeatherModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location icon",
                        modifier = Modifier.size(40.dp)
                    )
                    androidx.compose.material3.Text(text = data.location.name, fontSize = 30.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        text = data.location.country,
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Text(
                    text = " ${data.current.temp_c} ° c",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                AsyncImage(
                    modifier = Modifier.size(160.dp),
                    model = "https:${data.current.condition.icon}".replace("64x64","128x128"),
                    contentDescription = "Condition icon"
                )
                androidx.compose.material3.Text(
                    text = data.current.condition.text,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            WeatherKeyVal("Humidity",data.current.humidity)
                            WeatherKeyVal("Wind Speed",data.current.wind_kph+" km/h")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            WeatherKeyVal("UV",data.current.uv)
                            WeatherKeyVal("Participation",data.current.precip_mm+" mm")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            WeatherKeyVal("Local Time",data.location.localtime.split(" ")[1])
                            WeatherKeyVal("Local Date",data.location.localtime.split(" ")[0])
                        }
                    }
                }
                Button(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
        }
    }
}
