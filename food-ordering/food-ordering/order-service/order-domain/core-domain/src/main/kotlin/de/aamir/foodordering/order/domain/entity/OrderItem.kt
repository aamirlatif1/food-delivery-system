package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.entity.BaseEntity
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.order.domain.valueobject.OrderItemId

class OrderItem(
    id: OrderItemId? = null,
    product: Product,
    val quantity: Int,
    val price: Money,
    val subTotal: Money,
) : BaseEntity<OrderItemId>(id) {

    var product: Product = product
        private set

    fun isPriceValid(): Boolean =
        quantity > 0 && price.isGreaterThanZero() && price == product.price && subTotal == price * quantity

    // Called by Order.initializeOrder().
    internal fun initialize(id: OrderItemId) {
        this.id = id
    }

    // Called by OrderDomainService: replaces the requested product with the restaurant's
    // product of the same id, so the item carries the menu's name and price.
    internal fun confirmProduct(menuProduct: Product) {
        check(menuProduct.id == product.id) { "Product ${menuProduct.id} does not match ${product.id}" }
        product = menuProduct
    }

    class Builder {
        private var id: OrderItemId? = null
        private var product: Product? = null
        private var quantity: Int? = null
        private var price: Money? = null
        private var subTotal: Money? = null

        fun id(id: OrderItemId) = apply { this.id = id }

        fun product(product: Product) = apply { this.product = product }

        fun quantity(quantity: Int) = apply { this.quantity = quantity }

        fun price(price: Money) = apply { this.price = price }

        fun subTotal(subTotal: Money) = apply { this.subTotal = subTotal }

        fun build(): OrderItem = OrderItem(
            id = id,
            product = checkNotNull(product) { "OrderItem product is required" },
            quantity = checkNotNull(quantity) { "OrderItem quantity is required" },
            price = checkNotNull(price) { "OrderItem price is required" },
            subTotal = checkNotNull(subTotal) { "OrderItem subTotal is required" },
        )
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
