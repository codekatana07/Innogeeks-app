package edu.kiet.innogeeks

class Student {
    var stdName: String? = null // Automatically generates getters and setters
    var assigned: Boolean = false // Automatically generates getters and setters
    var stdID: String? = null // Automatically generates getters and setters
    var attendance: MutableMap<String, Boolean> = HashMap()
    var studentGrade: String? = null
    var studentSection: String? = null
    var counter: Int = 0
    var percentage: Double = 0.0 // Fixed spelling to "percentage"

    // Default constructor
    constructor()

    // Constructor with name, assigned, and ID
    constructor(stdName: String, assigned: Boolean, stdID: String) {
        this.stdName = stdName
        this.assigned = assigned
        this.stdID = stdID
    }

    // Constructor with only name
    constructor(stdName: String) {
        this.stdName = stdName
    }

    // Constructor with name and ID
    constructor(stdName: String, stdID: String) {
        this.stdName = stdName
        this.stdID = stdID
    }

    // Constructor with ID, grade, name, and section
    constructor(stdID: String, grade: String, name: String, section: String) {
        this.stdID = stdID
        this.stdName = name
        this.studentGrade = grade
        this.studentSection = section
        attendance = HashMap()
        counter = 0
    }

    // Method to add attendance
    fun addAttendance(date: String, value: Boolean) {
        attendance[date] = value
        counter++
    }

    // Calculate attendance percentage
    fun calculateAttendance() {
        var presentCount = 0
        for ((_, value) in attendance) {
            if (value) {
                presentCount++
            }
        }
        percentage = if (counter > 0) presentCount / (counter.toDouble()) else 0.0
    }

    // Check attendance for a specific date and value
    private fun check(dateValue: String, value: Boolean): Boolean {
        return attendance[dateValue] == value
    }

    // Get report based on criteria
    fun getReport(presentValue: String, gradeValue: String, dateValue: String, sectionValue: String): Boolean {
        return if (presentValue == "Present") {
            (gradeValue == "Any" || gradeValue == studentGrade) &&
                    check(dateValue, true) &&
                    (sectionValue == "Any" || sectionValue == studentSection)
        } else {
            (gradeValue == "Any" || gradeValue == studentGrade) &&
                    check(dateValue, false) &&
                    (sectionValue == "Any" || sectionValue == studentSection)
        }
    }

    // Override toString for a descriptive representation
    override fun toString(): String {
        return "$stdName is in $studentSection and $studentGrade with attendance $attendance"
    }
}
