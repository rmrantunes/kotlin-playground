package org.example.coroutines

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        val channel = Channel<Order>()

        val producerJob = launch {
            repeat(10) {
                launch {
                    delay(Random.nextLong(100, 500))
                    channel.send(generateOrder())
                }
            }
        }

        val processorsJob = launch {
            for (order in channel) {
                processOrder(order)
            }
        }

        producerJob.join()
        channel.close()
        processorsJob.join()
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
