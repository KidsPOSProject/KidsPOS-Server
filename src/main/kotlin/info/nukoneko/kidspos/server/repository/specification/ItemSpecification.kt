package info.nukoneko.kidspos.server.repository.specification

import info.nukoneko.kidspos.server.entity.ItemEntity
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object ItemSpecification {

    fun hasBarcode(barcode: String): Specification<ItemEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get<String>("barcode"), barcode)
        }
    }

    fun nameLike(name: String): Specification<ItemEntity> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                "%${name.lowercase()}%"
            )
        }
    }

    fun priceRange(minPrice: Int?, maxPrice: Int?): Specification<ItemEntity> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            minPrice?.let {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("price"), it)
                )
            }

            maxPrice?.let {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("price"), it)
                )
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }

    fun combine(vararg specs: Specification<ItemEntity>): Specification<ItemEntity> {
        return Specification { root, query, criteriaBuilder ->
            val predicates = specs.mapNotNull {
                it.toPredicate(root, query, criteriaBuilder)
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}