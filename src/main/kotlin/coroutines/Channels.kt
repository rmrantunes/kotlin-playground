package org.example.coroutines

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.selects.selectUnbiased

data class Item(val name: String, val price: Double)

data class Order(val id: Int, val items: List<Item>)

/**
 * Requisitos Específicos:
 *
 * 1 - Use kotlinx.coroutines e crie um Channel<Pedido> (onde Pedido é uma data class
 * personalizada).
 *
 * 2 - O produtor deve gerar 10 pedidos aleatórios, cada um com uma lista de itens (pelo menos 2-5
 * itens por pedido). Cada item tem um nome e preço (use valores randômicos entre 10.0 e 100.0).
 *
 * 3 - O consumidor deve receber os pedidos do channel, calcular o total do pedido, aplicar 10% de
 * desconto se o total > 200.0, e imprimir o resultado formatado.
 *
 * 4 - Feche o channel após o produtor terminar de enviar todos os pedidos.
 *
 * 5 - Use launch para coroutines e garanta que o programa espere o processamento completo antes de
 * terminar (use runBlocking ou similar).
 *
 * 6 - Adicione um atraso aleatório (100-500ms) no produtor para simular tempo de geração de
 * pedidos.
 */
class OrderProcessingChallenge {
    private val itemNames = listOf("Banana", "Shoe", "T-Shirt", "Macbook", "Beef")
    private val atomicInteger = AtomicInteger(0)

    suspend fun execute() = coroutineScope {
        val dataChannel = Channel<Order>(16)
        val errorChannel = Channel<Throwable>(Channel.UNLIMITED)

        val producerSupervisor = SupervisorJob()
        val producerScope = CoroutineScope(coroutineContext + producerSupervisor)
        val producerJob =
            producerScope.launch {
                buildList {
                        repeat(10) {
                            add(
                                producerScope.launch {
                                    try {
                                        delay(Random.nextLong(100, 500))
                                        dataChannel.send(generateOrder())
                                    } catch (e: Throwable) {
                                        errorChannel.send(e)
                                        throw e
                                    }
                                }
                            )
                        }
                    }
                    .joinAll()
            }

        val processorsJob = launch {
            try {
                for (order in dataChannel) {
                    processOrder(order)
                }
            } catch (e: Throwable) {
                errorChannel.send(e)
                throw e
            }
        }

        producerJob.join()
        dataChannel.close()
        processorsJob.join()
        errorChannel.close()

        errorChannel.toList().firstOrNull()?.let { throw it }

        println("All orders processed")
    }

    private fun generateItem(): Item {
        return Item(
            itemNames[Random.nextInt(0, itemNames.lastIndex)],
            Random.nextDouble(10.0, 100.0),
        )
    }

    private fun generateOrder(): Order {
        val id = atomicInteger.incrementAndGet()
        val items = buildList { repeat(Random.nextInt(2, 5)) { add(generateItem()) } }
        return Order(id, items)
    }

    private fun processOrder(order: Order) {
        val totalPrice = order.items.sumOf { it.price }
        val isEligibleForDiscount = totalPrice > 200.0
        val discountedPrice = totalPrice * 0.9
        val discountText =
            if (!isEligibleForDiscount) {
                "No discount applied"
            } else {
                "With discount: %.2f".format(discountedPrice)
            }
        println(
            "Processing order ${order.id}: Total without discount: %.2f - $discountText"
                .format(totalPrice)
        )
    }
}

// --- DATA ---
enum class OrderSide {
    BUY,
    SELL,
}

enum class EngineSpeed {
    FAST,
    MEDIUM,
    SLOW,
}

data class MarketTick(
    val id: Int,
    val symbol: String,
    val price: Double,
    val side: OrderSide,
    val timestamp: Long = System.currentTimeMillis(),
)

class StockMarketRouterChallenge {
    private val symbols = listOf("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA")
    private val idGenerator = AtomicInteger(0)
    private val processedCount = AtomicInteger(0)
    private val droppedCount = AtomicInteger(0)
    private val restartCount = AtomicInteger(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun execute() = coroutineScope {
        withTimeoutOrNull(10_000) {
            // TickGenerator
            val marketFeedChannel = produce {
                while (isActive) {
                    send(generateTick())
                    delay(1)
                }
            }

            launch { router(marketFeedChannel) }.join()
            marketFeedChannel.cancel()
        }

        println("Processed: ${processedCount.get()}")
        println("Restarted: ${restartCount.get()}")
    }

    private suspend fun router(marketFeedChannel: ReceiveChannel<MarketTick>) = coroutineScope {
        val supervisorJob = SupervisorJob()
        val supervisorScope = CoroutineScope(coroutineContext + supervisorJob)

        val routerFlow =
            MutableSharedFlow<MarketTick>(1, 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val highPriority = Channel<MarketTick>(64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val lowPriority = Channel<MarketTick>(64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        fun launchEngine(
            engineSpeed: EngineSpeed,
            engineFlow: MutableSharedFlow<MarketTick>,
            restart: Boolean = false,
        ) {
            supervisorScope.launch {
                val middleStageFlowJob =
                    routerFlow
                        .filter {
                            when (engineSpeed) {
                                EngineSpeed.FAST -> it.side == OrderSide.BUY
                                EngineSpeed.MEDIUM -> it.symbol == "MSFT"
                                else -> true
                            }
                        }
                        .onEach { engineFlow.emit(it) }
                        .launchIn(this)

                try {
                    generateEngine(engineSpeed, engineFlow.asSharedFlow(), restart)
                } catch (e: Exception) {
                    println("Exception while launching engine: $e")
                    middleStageFlowJob.cancel()
                    launchEngine(engineSpeed, engineFlow, restart = true)
                }
            }
        }

        launch {
            for (marketTick in marketFeedChannel) {
                if (marketTick.side == OrderSide.BUY) highPriority.send(marketTick)
                else lowPriority.send(marketTick)
            }
        }

        launch {
            while (isActive) {
                selectUnbiased {
                    highPriority.onReceive { routerFlow.emit(it) }
                    lowPriority.onReceive { routerFlow.emit(it) }
                }
            }
        }

        EngineSpeed.entries.forEach { speed ->
            run {
                val engineFlow =
                    MutableSharedFlow<MarketTick>(
                        1,
                        64,
                        onBufferOverflow = BufferOverflow.DROP_OLDEST,
                    )
                launchEngine(speed, engineFlow)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun generateEngine(
        engineSpeed: EngineSpeed,
        engineFlow: SharedFlow<MarketTick>,
        restart: Boolean = false,
    ) = coroutineScope {
        val isSlow = engineSpeed == EngineSpeed.SLOW

        fun jeopardizeCrash() {
            if (Random.nextInt(1, 100) > 95) throw RuntimeException("Too much, baby")
        }

        suspend fun process(tick: MarketTick) {
            val delayTime =
                when (engineSpeed) {
                    EngineSpeed.FAST -> Random.nextLong(10, 50)
                    EngineSpeed.MEDIUM -> Random.nextLong(50, 150)
                    EngineSpeed.SLOW -> Random.nextLong(200, 300)
                }

            delay(delayTime)
            if (isSlow) jeopardizeCrash()
            println(
                "[$engineSpeed engine] #${tick.id} ${tick.side} ${tick.symbol} @ %.2f | ${tick.timestamp}"
                    .format(tick.price)
            )
            processedCount.incrementAndGet()
        }

        val cacheTick = engineFlow.replayCache.firstOrNull()
        launch {
            if (restart && cacheTick != null) {
                println("[$engineSpeed engine] Restating engine...")
                restartCount.incrementAndGet()
                process(cacheTick)
            }

            engineFlow.distinctUntilChangedBy { "${it.symbol} ${it.side}" }.collect { process(it) }
        }
    }

    private fun generateTick(): MarketTick {
        return MarketTick(
            id = idGenerator.incrementAndGet(),
            symbol = symbols.random(),
            price = Random.nextDouble(100.0, 1000.0),
            side = if (Random.nextBoolean()) OrderSide.BUY else OrderSide.SELL,
        )
    }
}
