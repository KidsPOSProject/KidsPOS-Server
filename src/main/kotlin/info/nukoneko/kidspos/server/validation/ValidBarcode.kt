package info.nukoneko.kidspos.server.validation

import info.nukoneko.kidspos.common.Constants
import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [BarcodeValidator::class])
annotation class ValidBarcode(
    val message: String = "Invalid barcode format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class BarcodeValidator : ConstraintValidator<ValidBarcode, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true // Let @NotNull handle null checks
        }

        return value.matches(Regex(Constants.Validation.BARCODE_PATTERN)) &&
               value.length >= Constants.Barcode.MIN_LENGTH
    }
}