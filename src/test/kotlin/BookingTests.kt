import dev.forkhandles.result4k.Result4k
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

class BookARoomCommandTest {
    val someDate = LocalDate.now().plusDays(1)

    @Test
    fun `if a booking command is successful,then the room is booked`() {
        val theRooms = listOf(Room(101, RoomType.Single))
        var repo = InMemoryBookingRepository(theRooms)

        val command = BookARoomCommand(repo)
        command.execute(someDate)
        assertThat(repo.availableRooms(someDate), equalTo(AvailableRooms()))
    }

    @Test
    fun `if booking command fails, then available rooms stays the same`() {
        val theRooms = listOf(Room(101, RoomType.Single))
        var repo = InMemoryBookingRepository(theRooms)
        val command = BookARoomCommand(repo)
        command.execute(someDate)
        command.execute(someDate)
        assertThat(repo.availableRooms(someDate), equalTo(AvailableRooms()))
    }
}

class OccupationTest {
    val dirk = Person("Dirk")
    val jim = Person("Jim")
    val room1 = Room(1, RoomType.Single)
    val room2 = Room(2, RoomType.Twin)

    @Test
    fun `person can not be checked in for two room`() {
        val occupation = Occupation(
            mapOf(dirk to room1)
        )
        val expectedError = CheckInError("Dirk can not be checked into room 2")
        assertThat(occupation.checkin(dirk, room2), equalTo(expectedError.asFailure()))
    }

    @Test
    fun `person is checked in after checking in`() {
        val occupation = Occupation(emptyMap())
        assertThat(occupation.checkin(jim, room1), equalTo(PersonWasCheckIn(jim,room1).asSuccess()))
    }

    @Test
    fun `person can not be checked in single room when room is already occupied`() {
        val occupation = Occupation(mapOf(dirk to room1))
        val expectedError = CheckInError("Jim can not be checked into room 1")
        assertThat(occupation.checkin(jim, room1), equalTo(expectedError.asFailure()))
    }

    @Test
    fun `two persons can be checked into twin room`() {
        val occupation = Occupation(mapOf(dirk to room2))
        assertThat(occupation.checkin(jim, room2), equalTo(PersonWasCheckIn(jim,room2).asSuccess()))
    }
}

data class Person(val name: String)
data class CheckInError(val message: String)
data class PersonWasCheckIn(val jim: Person, val room: Room)


data class Occupation(val personToRooms: Map<Person, Room>) {
    fun checkin(person: Person, room:Room): Result4k<PersonWasCheckIn, CheckInError> {
        val roomIsFull = personToRooms.containsValue(room) && room.roomType == RoomType.Single
        val personIsInRoom = personToRooms.containsKey(person)
        if (personIsInRoom || roomIsFull) {
            return CheckInError("${person.name} can not be checked into room ${room.roomNumber}").asFailure()
        }
        return PersonWasCheckIn(person, room).asSuccess()
    }
}


class BookARoomCommand(val repo: BookingRepository) {
    fun execute(someDate: LocalDate) {
        val availableRooms = repo.availableRooms(someDate)
        availableRooms.book(someDate).map {
            repo.save(it)
        }
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
