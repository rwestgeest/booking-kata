import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.map
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.time.LocalDate


class BookingTests {
    val bookingDate = LocalDate.now().plusDays(1)

    @Test
    fun `booking a room books the first available one`() {
        val the_room = Room(101, RoomType.Single)
        val availableRooms = AvailableRooms(listOf(the_room))
        assertThat(availableRooms.book(bookingDate), equalTo(RoomWasBooked(the_room, bookingDate).asSuccess()))
    }

    @Test
    fun `booking a room fails when there are no rooms available`() {
        val availableRooms = AvailableRooms()
        assertThat(availableRooms.book(bookingDate), equalTo(BookingError("no rooms available at $bookingDate").asFailure()))
    }

    @Test
    fun `booking a selects room with specfic type if requested`() {
        val availableRooms = AvailableRooms(listOf(Room(101, RoomType.Single), Room(102, RoomType.Twin)))
        assertThat(availableRooms.book(bookingDate, RoomType.Twin), equalTo(RoomWasBooked(Room(102, RoomType.Twin), bookingDate).asSuccess()))
    }
}

data class BookingError(val message: String)

data class RoomWasBooked(val theRoom: Room, val bookingDate: LocalDate)

data class AvailableRooms(val rooms: List<Room> = emptyList()) {
    fun book(bookingDate: LocalDate, requestedRoomType: RoomType? = null): Result4k<RoomWasBooked, BookingError> {
        val rooms = rooms.filter { requestedRoomType == null || it.roomType == requestedRoomType }

        if (rooms.isEmpty()) return BookingError("no rooms available at $bookingDate").asFailure()
        return RoomWasBooked(rooms.first(), bookingDate).asSuccess()
    }
}

data class Room(val roomNumber: Int, val roomType: RoomType)

enum class RoomType {
    Single,
    Twin

}
