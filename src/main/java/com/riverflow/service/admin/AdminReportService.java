package com.riverflow.service.admin;

import com.riverflow.dto.admin.ReportExportRequest;
import com.riverflow.dto.admin.ReportStatisticsResponse;
import com.riverflow.dto.admin.ReportTimeSeriesData;
import com.riverflow.model.User;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.repository.admin.AdminReportRepository;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Service for admin reporting and statistics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final AdminReportRepository adminReportRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final MindmapRepository mindmapRepository;
    private final MongoTemplate mongoTemplate;

    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /**
     * Get comprehensive statistics
     */
    public ReportStatisticsResponse getStatistics() {
        log.info("Fetching comprehensive report statistics");

        return ReportStatisticsResponse.builder()
                .userStats(getUserStatistics())
                .mindmapStats(getMindmapStatistics())
                .revenueStats(getRevenueStatistics())
                .build();
    }

    /**
     * Get time series data for charts
     */
    public ReportTimeSeriesData getTimeSeriesData(ReportTimeSeriesData.TimePeriod period,
            LocalDate startDate,
            LocalDate endDate) {
        log.info("Fetching time series data for period: {}", period);

        // Set default date range based on period if not provided
        if (startDate == null || endDate == null) {
            endDate = LocalDate.now();
            startDate = switch (period) {
                case DAILY -> endDate.minusDays(7);
                case WEEKLY -> endDate.minusWeeks(4);
                case MONTHLY -> endDate.minusMonths(12);
                case YEARLY -> endDate.minusYears(5);
            };
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<String> labels = new ArrayList<>();
        List<ReportTimeSeriesData.TimeSeriesPoint> userRegistrations = new ArrayList<>();
        List<ReportTimeSeriesData.TimeSeriesPoint> revenue = new ArrayList<>();
        List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations = new ArrayList<>();
        List<ReportTimeSeriesData.TimeSeriesPoint> transactions = new ArrayList<>();

        switch (period) {
            case DAILY -> {
                populateDailyData(startDateTime, endDateTime, labels, userRegistrations, revenue, mindmapCreations,
                        transactions);
            }
            case WEEKLY -> {
                populateWeeklyData(startDateTime, endDateTime, labels, userRegistrations, revenue, mindmapCreations,
                        transactions);
            }
            case MONTHLY -> {
                populateMonthlyData(startDateTime, endDateTime, labels, userRegistrations, revenue, mindmapCreations,
                        transactions);
            }
            case YEARLY -> {
                populateYearlyData(startDateTime, endDateTime, labels, userRegistrations, revenue, mindmapCreations,
                        transactions);
            }
        }

        return ReportTimeSeriesData.builder()
                .labels(labels)
                .userRegistrations(userRegistrations)
                .revenue(revenue)
                .mindmapCreations(mindmapCreations)
                .transactions(transactions)
                .build();
    }

    /**
     * Export report as specified format
     */
    public byte[] exportReport(ReportExportRequest request) {
        log.info("Exporting report: type={}, format={}", request.getReportType(), request.getFormat());

        ReportStatisticsResponse stats = getStatistics();

        return switch (request.getFormat()) {
            case CSV -> exportAsCsv(stats, request);
            case JSON -> exportAsJson(stats);
            case XLSX -> exportAsXlsx(stats, request);
        };
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private ReportStatisticsResponse.UserStatistics getUserStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
                .atStartOfDay();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastWeek = startOfWeek.minusWeeks(1);
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        Long totalUsers = adminReportRepository.countTotalUsers();
        Long activeUsers = adminReportRepository.countByStatus(User.UserStatus.active);
        Long suspendedUsers = adminReportRepository.countByStatus(User.UserStatus.suspended);
        Long deletedUsers = adminReportRepository.countByStatus(User.UserStatus.deleted);

        Long newUsersToday = adminReportRepository.countByCreatedAtAfter(startOfToday);
        Long newUsersThisWeek = adminReportRepository.countByCreatedAtAfter(startOfWeek);
        Long newUsersThisMonth = adminReportRepository.countByCreatedAtAfter(startOfMonth);

        Long adminCount = adminReportRepository.countByRole(User.Role.admin);
        Long superAdminCount = adminReportRepository.countByRole(User.Role.super_admin);
        Long regularUserCount = adminReportRepository.countByRole(User.Role.user);

        // Calculate growth percentages
        Long lastWeekUsers = adminReportRepository.countByCreatedAtBetween(startOfLastWeek, startOfWeek);
        Long lastMonthUsers = adminReportRepository.countByCreatedAtBetween(startOfLastMonth, startOfMonth);

        Double weeklyGrowth = calculateGrowthPercent(lastWeekUsers, newUsersThisWeek);
        Double monthlyGrowth = calculateGrowthPercent(lastMonthUsers, newUsersThisMonth);

        return ReportStatisticsResponse.UserStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .deletedUsers(deletedUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .adminCount(adminCount)
                .superAdminCount(superAdminCount)
                .regularUserCount(regularUserCount)
                .weeklyGrowthPercent(weeklyGrowth)
                .monthlyGrowthPercent(monthlyGrowth)
                .build();
    }

    private ReportStatisticsResponse.MindmapStatistics getMindmapStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
                .atStartOfDay();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();

        // MongoDB queries for mindmap statistics
        long totalMindmaps = mongoTemplate.count(new Query(), Mindmap.class);
        long activeMindmaps = mongoTemplate.count(Query.query(Criteria.where("status").is("active")), Mindmap.class);
        long archivedMindmaps = mongoTemplate.count(Query.query(Criteria.where("status").is("archived")),
                Mindmap.class);
        long deletedMindmaps = mongoTemplate.count(Query.query(Criteria.where("status").is("deleted")), Mindmap.class);

        long newMindmapsToday = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(startOfToday)), Mindmap.class);
        long newMindmapsThisWeek = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(startOfWeek)), Mindmap.class);
        long newMindmapsThisMonth = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(startOfMonth)), Mindmap.class);

        long publicMindmaps = mongoTemplate.count(Query.query(Criteria.where("isPublic").is(true)), Mindmap.class);
        long privateMindmaps = totalMindmaps - publicMindmaps;

        long aiGeneratedMindmaps = mongoTemplate.count(Query.query(Criteria.where("aiGenerated").is(true)),
                Mindmap.class);

        // Calculate growth percentages
        LocalDateTime startOfLastWeek = startOfWeek.minusWeeks(1);
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        long lastWeekMindmaps = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(startOfLastWeek).lt(startOfWeek)), Mindmap.class);
        long lastMonthMindmaps = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(startOfLastMonth).lt(startOfMonth)), Mindmap.class);

        Double weeklyGrowth = calculateGrowthPercent(lastWeekMindmaps, newMindmapsThisWeek);
        Double monthlyGrowth = calculateGrowthPercent(lastMonthMindmaps, newMindmapsThisMonth);

        return ReportStatisticsResponse.MindmapStatistics.builder()
                .totalMindmaps(totalMindmaps)
                .activeMindmaps(activeMindmaps)
                .archivedMindmaps(archivedMindmaps)
                .deletedMindmaps(deletedMindmaps)
                .newMindmapsToday(newMindmapsToday)
                .newMindmapsThisWeek(newMindmapsThisWeek)
                .newMindmapsThisMonth(newMindmapsThisMonth)
                .publicMindmaps(publicMindmaps)
                .privateMindmaps(privateMindmaps)
                .aiGeneratedMindmaps(aiGeneratedMindmaps)
                .weeklyGrowthPercent(weeklyGrowth)
                .monthlyGrowthPercent(monthlyGrowth)
                .build();
    }

    private ReportStatisticsResponse.RevenueStatistics getRevenueStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
                .atStartOfDay();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastWeek = startOfWeek.minusWeeks(1);
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        Long totalRevenue = paymentTransactionRepository.sumTotalProcessedRevenue();
        Long totalTransactions = paymentTransactionRepository.count();

        Long revenueToday = paymentTransactionRepository.sumProcessedRevenueAfter(startOfToday);
        Long revenueThisWeek = paymentTransactionRepository.sumProcessedRevenueAfter(startOfWeek);
        Long revenueThisMonth = paymentTransactionRepository.sumProcessedRevenueAfter(startOfMonth);

        Long transactionsToday = paymentTransactionRepository.countByCreatedAtAfter(startOfToday);
        Long transactionsThisWeek = paymentTransactionRepository.countByCreatedAtAfter(startOfWeek);
        Long transactionsThisMonth = paymentTransactionRepository.countByCreatedAtAfter(startOfMonth);

        Double averageValue = totalTransactions > 0 ? (double) totalRevenue / totalTransactions : 0.0;

        // Calculate growth percentages
        Long lastWeekRevenue = paymentTransactionRepository.sumProcessedAmountBetween(startOfLastWeek, startOfWeek);
        Long lastMonthRevenue = paymentTransactionRepository.sumProcessedAmountBetween(startOfLastMonth, startOfMonth);

        Double weeklyGrowth = calculateGrowthPercent(lastWeekRevenue, revenueThisWeek);
        Double monthlyGrowth = calculateGrowthPercent(lastMonthRevenue, revenueThisMonth);

        return ReportStatisticsResponse.RevenueStatistics.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .revenueToday(revenueToday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .transactionsToday(transactionsToday)
                .transactionsThisWeek(transactionsThisWeek)
                .transactionsThisMonth(transactionsThisMonth)
                .averageTransactionValue(averageValue)
                .weeklyGrowthPercent(weeklyGrowth)
                .monthlyGrowthPercent(monthlyGrowth)
                .build();
    }

    private Double calculateGrowthPercent(Long previous, Long current) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100;
    }

    private void populateDailyData(LocalDateTime startDateTime, LocalDateTime endDateTime,
            List<String> labels,
            List<ReportTimeSeriesData.TimeSeriesPoint> userRegistrations,
            List<ReportTimeSeriesData.TimeSeriesPoint> revenue,
            List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations,
            List<ReportTimeSeriesData.TimeSeriesPoint> transactions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        // Get user registrations
        List<Object[]> userDailyData = adminReportRepository.getDailyRegistrations(startDateTime, endDateTime);
        for (Object[] row : userDailyData) {
            String label = ((java.sql.Date) row[0]).toLocalDate().format(formatter);
            Long count = ((Number) row[1]).longValue();
            labels.add(label);
            userRegistrations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(count.toString())
                    .build());
        }

        // Get revenue data
        List<Object[]> revenueDailyData = paymentTransactionRepository.getDailyRevenue(startDateTime, endDateTime);
        for (Object[] row : revenueDailyData) {
            String label = ((java.sql.Date) row[0]).toLocalDate().format(formatter);
            Long amount = ((Number) row[1]).longValue();
            Long count = ((Number) row[2]).longValue();
            revenue.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(amount)
                    .formattedValue(VND_FORMAT.format(amount) + " VND")
                    .build());
            transactions.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(count.toString())
                    .build());
        }

        // Get mindmap creations from MongoDB
        populateMindmapDailyData(startDateTime, endDateTime, mindmapCreations);
    }

    private void populateWeeklyData(LocalDateTime startDateTime, LocalDateTime endDateTime,
            List<String> labels,
            List<ReportTimeSeriesData.TimeSeriesPoint> userRegistrations,
            List<ReportTimeSeriesData.TimeSeriesPoint> revenue,
            List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations,
            List<ReportTimeSeriesData.TimeSeriesPoint> transactions) {
        // Get user registrations
        List<Object[]> userWeeklyData = adminReportRepository.getWeeklyRegistrations(startDateTime, endDateTime);
        for (Object[] row : userWeeklyData) {
            String label = "W" + row[1] + "/" + row[0];
            Long count = ((Number) row[2]).longValue();
            labels.add(label);
            userRegistrations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(count.toString())
                    .build());
        }

        // Get revenue data
        List<Object[]> revenueWeeklyData = paymentTransactionRepository.getWeeklyRevenue(startDateTime, endDateTime);
        for (Object[] row : revenueWeeklyData) {
            String label = "W" + row[1] + "/" + row[0];
            Long amount = ((Number) row[2]).longValue();
            Long count = ((Number) row[3]).longValue();
            revenue.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(amount)
                    .formattedValue(VND_FORMAT.format(amount) + " VND")
                    .build());
            transactions.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(count.toString())
                    .build());
        }

        // For mindmaps, aggregate from MongoDB
        populateMindmapWeeklyData(startDateTime, endDateTime, mindmapCreations);
    }

    private void populateMonthlyData(LocalDateTime startDateTime, LocalDateTime endDateTime,
            List<String> labels,
            List<ReportTimeSeriesData.TimeSeriesPoint> userRegistrations,
            List<ReportTimeSeriesData.TimeSeriesPoint> revenue,
            List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations,
            List<ReportTimeSeriesData.TimeSeriesPoint> transactions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        // Get user registrations
        List<Object[]> userMonthlyData = adminReportRepository.getMonthlyRegistrations(startDateTime, endDateTime);
        for (Object[] row : userMonthlyData) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String label = LocalDate.of(year, month, 1).format(formatter);
            Long count = ((Number) row[2]).longValue();
            labels.add(label);
            userRegistrations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(count.toString())
                    .build());
        }

        // Get revenue data
        List<Object[]> revenueMonthlyData = paymentTransactionRepository.getMonthlyRevenue(startDateTime, endDateTime);
        for (Object[] row : revenueMonthlyData) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String label = LocalDate.of(year, month, 1).format(formatter);
            Long amount = ((Number) row[2]).longValue();
            Long count = ((Number) row[3]).longValue();
            revenue.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(amount)
                    .formattedValue(VND_FORMAT.format(amount) + " VND")
                    .build());
            transactions.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(count.toString())
                    .build());
        }

        populateMindmapMonthlyData(startDateTime, endDateTime, mindmapCreations);
    }

    private void populateYearlyData(LocalDateTime startDateTime, LocalDateTime endDateTime,
            List<String> labels,
            List<ReportTimeSeriesData.TimeSeriesPoint> userRegistrations,
            List<ReportTimeSeriesData.TimeSeriesPoint> revenue,
            List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations,
            List<ReportTimeSeriesData.TimeSeriesPoint> transactions) {
        // Yearly data - simplified aggregation
        LocalDate start = startDateTime.toLocalDate().withDayOfYear(1);
        LocalDate end = endDateTime.toLocalDate();

        while (!start.isAfter(end)) {
            String label = String.valueOf(start.getYear());
            LocalDateTime yearStart = start.atStartOfDay();
            LocalDateTime yearEnd = start.withDayOfYear(start.lengthOfYear()).atTime(LocalTime.MAX);

            labels.add(label);

            Long userCount = adminReportRepository.countByCreatedAtBetween(yearStart, yearEnd);
            userRegistrations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(userCount)
                    .formattedValue(userCount.toString())
                    .build());

            Long revenueAmount = paymentTransactionRepository.sumProcessedAmountBetween(yearStart, yearEnd);
            revenue.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(revenueAmount)
                    .formattedValue(VND_FORMAT.format(revenueAmount) + " VND")
                    .build());

            Long txCount = paymentTransactionRepository.countByCreatedAtBetween(yearStart, yearEnd);
            transactions.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(txCount)
                    .formattedValue(txCount.toString())
                    .build());

            long mindmapCount = mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(yearStart).lte(yearEnd)), Mindmap.class);
            mindmapCreations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(mindmapCount)
                    .formattedValue(String.valueOf(mindmapCount))
                    .build());

            start = start.plusYears(1);
        }
    }

    private void populateMindmapDailyData(LocalDateTime startDateTime, LocalDateTime endDateTime,
            List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        LocalDate start = startDateTime.toLocalDate();
        LocalDate end = endDateTime.toLocalDate();

        while (!start.isAfter(end)) {
            LocalDateTime dayStart = start.atStartOfDay();
            LocalDateTime dayEnd = start.atTime(LocalTime.MAX);
            String label = start.format(formatter);

            long count = mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(dayStart).lte(dayEnd)), Mindmap.class);

            mindmapCreations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(String.valueOf(count))
                    .build());

            start = start.plusDays(1);
        }
    }

    private void populateMindmapWeeklyData(LocalDateTime startDateTime, LocalDateTime endDateTime,
            List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations) {
        LocalDate start = startDateTime.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = endDateTime.toLocalDate();

        while (!start.isAfter(end)) {
            LocalDateTime weekStart = start.atStartOfDay();
            LocalDateTime weekEnd = start.plusDays(6).atTime(LocalTime.MAX);
            String label = "W" + start.get(java.time.temporal.WeekFields.ISO.weekOfYear()) + "/" + start.getYear();

            long count = mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(weekStart).lte(weekEnd)), Mindmap.class);

            mindmapCreations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(String.valueOf(count))
                    .build());

            start = start.plusWeeks(1);
        }
    }

    private void populateMindmapMonthlyData(LocalDateTime startDateTime, LocalDateTime endDateTime,
            List<ReportTimeSeriesData.TimeSeriesPoint> mindmapCreations) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        LocalDate start = startDateTime.toLocalDate().withDayOfMonth(1);
        LocalDate end = endDateTime.toLocalDate();

        while (!start.isAfter(end)) {
            LocalDateTime monthStart = start.atStartOfDay();
            LocalDateTime monthEnd = start.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
            String label = start.format(formatter);

            long count = mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(monthStart).lte(monthEnd)), Mindmap.class);

            mindmapCreations.add(ReportTimeSeriesData.TimeSeriesPoint.builder()
                    .label(label)
                    .value(count)
                    .formattedValue(String.valueOf(count))
                    .build());

            start = start.plusMonths(1);
        }
    }

    // ==================== EXPORT METHODS ====================

    private byte[] exportAsCsv(ReportStatisticsResponse stats, ReportExportRequest request) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // Add BOM for Excel UTF-8 compatibility
        writer.print('\uFEFF');

        // Header
        writer.println("Report Statistics - Generated at "
                + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        writer.println();

        // User Statistics
        if (request.getReportType() == ReportExportRequest.ReportType.USERS ||
                request.getReportType() == ReportExportRequest.ReportType.ALL) {
            writer.println("USER STATISTICS");
            writer.println("Metric,Value");
            writer.println("Total Users," + stats.getUserStats().getTotalUsers());
            writer.println("Active Users," + stats.getUserStats().getActiveUsers());
            writer.println("Suspended Users," + stats.getUserStats().getSuspendedUsers());
            writer.println("New Users Today," + stats.getUserStats().getNewUsersToday());
            writer.println("New Users This Week," + stats.getUserStats().getNewUsersThisWeek());
            writer.println("New Users This Month," + stats.getUserStats().getNewUsersThisMonth());
            writer.println("Weekly Growth %," + String.format("%.2f", stats.getUserStats().getWeeklyGrowthPercent()));
            writer.println("Monthly Growth %," + String.format("%.2f", stats.getUserStats().getMonthlyGrowthPercent()));
            writer.println();
        }

        // Mindmap Statistics
        if (request.getReportType() == ReportExportRequest.ReportType.MINDMAPS ||
                request.getReportType() == ReportExportRequest.ReportType.ALL) {
            writer.println("MINDMAP STATISTICS");
            writer.println("Metric,Value");
            writer.println("Total Mindmaps," + stats.getMindmapStats().getTotalMindmaps());
            writer.println("Active Mindmaps," + stats.getMindmapStats().getActiveMindmaps());
            writer.println("Public Mindmaps," + stats.getMindmapStats().getPublicMindmaps());
            writer.println("AI Generated," + stats.getMindmapStats().getAiGeneratedMindmaps());
            writer.println("New Mindmaps Today," + stats.getMindmapStats().getNewMindmapsToday());
            writer.println("New Mindmaps This Week," + stats.getMindmapStats().getNewMindmapsThisWeek());
            writer.println("New Mindmaps This Month," + stats.getMindmapStats().getNewMindmapsThisMonth());
            writer.println();
        }

        // Revenue Statistics
        if (request.getReportType() == ReportExportRequest.ReportType.REVENUE ||
                request.getReportType() == ReportExportRequest.ReportType.ALL) {
            writer.println("REVENUE STATISTICS");
            writer.println("Metric,Value");
            writer.println("Total Revenue," + VND_FORMAT.format(stats.getRevenueStats().getTotalRevenue()) + " VND");
            writer.println("Total Transactions," + stats.getRevenueStats().getTotalTransactions());
            writer.println("Revenue Today," + VND_FORMAT.format(stats.getRevenueStats().getRevenueToday()) + " VND");
            writer.println(
                    "Revenue This Week," + VND_FORMAT.format(stats.getRevenueStats().getRevenueThisWeek()) + " VND");
            writer.println(
                    "Revenue This Month," + VND_FORMAT.format(stats.getRevenueStats().getRevenueThisMonth()) + " VND");
            writer.println("Average Transaction,"
                    + VND_FORMAT.format(stats.getRevenueStats().getAverageTransactionValue().longValue()) + " VND");
            writer.println(
                    "Weekly Growth %," + String.format("%.2f", stats.getRevenueStats().getWeeklyGrowthPercent()));
            writer.println(
                    "Monthly Growth %," + String.format("%.2f", stats.getRevenueStats().getMonthlyGrowthPercent()));
        }

        writer.flush();
        return baos.toByteArray();
    }

    private byte[] exportAsJson(ReportStatisticsResponse stats) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(stats);
        } catch (Exception e) {
            log.error("Error exporting as JSON", e);
            return "{}".getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] exportAsXlsx(ReportStatisticsResponse stats, ReportExportRequest request) {
        // For simplicity, return CSV format with xlsx extension
        // A full implementation would use Apache POI
        return exportAsCsv(stats, request);
    }
}
