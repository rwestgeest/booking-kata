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

class BookingRepository_AvailableRoomsTest {
    val someDate = LocalDate.now().plusDays(1)
    val theRooms = listOf(Room(101, RoomType.Single), Room(102, RoomType.Twin))
    var repo = InMemoryBookingRepository(theRooms)

    @Test
    fun `contains all room by default`() {
        assertThat(repo.availableRooms(someDate), equalTo(AvailableRooms(theRooms)))
    }

    @Test
    fun `if a booking is done for a room, it is no longer available (but the other rooms are)`() {
        repo.save(RoomWasBooked(theRooms.first(), someDate))
        assertThat(repo.availableRooms(someDate), equalTo(AvailableRooms(listOf(theRooms.last()))))
    }

    @Test
    fun `if a booking is done the room is still available on a different date`() {
        repo.save(RoomWasBooked(theRooms.first(), someDate))
        assertThat(repo.availableRooms(someDate.plusDays(1)), equalTo(AvailableRooms(theRooms)))
    }
}


interface BookingRepository {
    fun availableRooms(someDate: LocalDate): AvailableRooms
    fun save(roomWasBooked: RoomWasBooked)
}

data class InMemoryBookingRepository(val theRooms: List<Room>) : BookingRepository {
    val bookings = mutableListOf<RoomWasBooked>()

    override fun availableRooms(someDate: LocalDate): AvailableRooms {
        val availableRooms = theRooms.filter { it.roomNumber !in bookedRoomNumbersAt(someDate) }
        return AvailableRooms(availableRooms)
    }

    private fun bookedRoomNumbersAt(someDate: LocalDate): List<Int> {
        return bookingsAt(someDate).map { it.theRoom.roomNumber }
    }

    private fun bookingsAt(someDate: LocalDate): List<RoomWasBooked> {
        return bookings.filter { it.bookingDate == someDate }
    }

    override fun save(roomWasBooked: RoomWasBooked) {
        bookings.add(roomWasBooked)
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
