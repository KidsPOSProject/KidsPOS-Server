package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.repository.StaffRepository
import org.springframework.beans.factory.annotation.Autowired
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

    fun findStaff(barcode: String): StaffEntity? {
        return repository.findByIdOrNull(barcode)
    }
}