package com.example.telegramreporttool.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getReportsForAccount(accountId: Long): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Delete
    suspend fun deleteReport(report: ReportEntity)

    @Query("SELECT * FROM templates")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun deactivateAllAccounts()

    @Query("UPDATE accounts SET isActive = 1 WHERE userId = :userId")
    suspend fun activateAccount(userId: Long)

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): AccountEntity?

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    // Proxy operations
    @Query("SELECT * FROM proxies")
    fun getAllProxies(): Flow<List<ProxyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxy(proxy: ProxyEntity)

    @Update
    suspend fun updateProxy(proxy: ProxyEntity)

    @Delete
    suspend fun deleteProxy(proxy: ProxyEntity)

    @Query("UPDATE proxies SET isEnabled = 0")
    suspend fun disableAllProxies()

    @Query("UPDATE proxies SET isEnabled = 1 WHERE id = :proxyId")
    suspend fun enableProxy(proxyId: Int)

    @Query("SELECT * FROM proxies WHERE isEnabled = 1 LIMIT 1")
    suspend fun getEnabledProxy(): ProxyEntity?
}
