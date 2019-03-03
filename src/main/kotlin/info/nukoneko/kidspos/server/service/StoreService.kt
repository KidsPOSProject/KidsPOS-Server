package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.api.model.StoreBean
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class StoreService {

    @Autowired
    private lateinit var repository: StoreRepository

    fun findAll(): List<StoreEntity> {
        return repository.findAll()
    }

    fun findStore(id: Int): StoreEntity? {
        return repository.findByIdOrNull(id)
    }

    fun save(storeBean: StoreBean): StoreEntity {
        val id = try {
            repository.getLastId() + 1
        } catch (e: Throwable) {
            1
        }
        val store = StoreEntity(id, storeBean.name, storeBean.printerUri)
        return repository.save(store)
    }
}