package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.api.model.StaffBean
import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.repository.StaffRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class StaffService {

    @Autowired
    private lateinit var repository: StaffRepository

    fun findAll(): List<StaffEntity> {
        return repository.findAll()
    }

    fun findStaff(id: Int): StaffEntity? {
        return repository.findByIdOrNull(id)
    }

    fun findStaff(barcode: String): StaffEntity? {
        val id = barcode.substring(barcode.length - 3).toInt()
        return repository.findByIdOrNull(id)
    }

    fun save(staffBean: StaffBean): StaffEntity {
        val id = try {
            repository.getLastId() + 1
        } catch (e: EmptyResultDataAccessException) {
            1
        }
        val staff = StaffEntity(id, staffBean.barcode, staffBean.name)
        return repository.save(staff)
    }
}