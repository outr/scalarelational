package org.scalarelational.extra

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalarelational.model.SQLDatastore

trait HikariSupport extends SQLDatastore {
  // Automatically converts DataSources to be wrapped by HikariDataSource
  dataSourceProperty.filterCycles.attach {
    case Some(ds: HikariDataSource) =>
      // Ignore HikariDataSource

    case Some(ds) =>
      val config = new HikariConfig()
      config.setDataSource(ds)

      val hikari = new HikariDataSource(config)
      dataSourceProperty := hikari

    case _ =>
  }

  /** Clean up data source after shutdown */
  override def dispose() {
    super.dispose()

    dataSource match {
      case Some(ds: HikariDataSource) => ds.close()
      case _ =>  // Not a HikariDataSource
    }
  }
}