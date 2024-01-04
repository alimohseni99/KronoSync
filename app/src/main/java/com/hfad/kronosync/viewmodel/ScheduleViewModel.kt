import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hfad.kronosync.repository.ScheduleRepository
import com.hfad.kronosync.ui.ScheduleItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {
    private var currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
    private var currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val scheduleItems = MutableLiveData<Result<List<ScheduleItem>>>()

    fun fetchSchedule(programCode: String) {
        val url = getScheduleUrl(programCode)
        repository.fetchScheduleData(url) { result ->
            val scheduleResult = result.map { list ->
                list.map { eventItem ->
                    ScheduleItem(
                        startTime = eventItem.startTime,
                        endTime = eventItem.endTime,
                        title = eventItem.courseName,
                        location = eventItem.locationId,
                        description = eventItem.moment
                    )
                }
            }
            scheduleItems.postValue(scheduleResult)
            print(scheduleItems)

        }
    }

    fun getCurrentWeek(): Int {
        return currentWeek
    }

    fun getCurrentDayOfWeek(): Int {
        return currentDayOfWeek
    }


    fun setCurrentWeek(week: Int) {
        currentWeek = week
    }

    fun setCurrentDayOfWeek(dayOfWeek: Int) {
        currentDayOfWeek = dayOfWeek
    }


    private fun getScheduleUrl(programCode: String): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.WEEK_OF_YEAR, currentWeek)
        calendar.set(Calendar.DAY_OF_WEEK, currentDayOfWeek)

        val startDatum = SimpleDateFormat("yyyy-MM-dd", Locale("sv", "SE")).format(calendar.time)

        return "https://schema.hig.se/setup/jsp/SchemaXML.jsp?startDatum=$startDatum&intervallTyp=d&intervallAntal=1&forklaringar=true&sokMedAND=true&sprak=SV&resurser=p.$programCode"
    }
}


