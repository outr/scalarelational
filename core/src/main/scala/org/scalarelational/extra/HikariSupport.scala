package org.scalarelational.extra

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalarelational.model.SQLDatastore


trait HikariSupport extends SQLDatastore {
  // Automatically converts DataSources to be wrapped by HikariDataSource
  dataSourceProperty.change.on {
    case evt => evt.newValue match {
      case null => // Ignore null DataSource
      case ds: HikariDataSource => // Ignore HikariDataSource
      case ds => {
        val config = new HikariConfig()
        config.setDataSource(ds)
        val hikari = new HikariDataSource(config)
        dataSourceProperty := hikari
      }
    }
  }

  // Clean up data source after shutdown
  override def dispose() = {
    super.dispose()

    dataSource match {
      case ds: HikariDataSource => ds.close()
      case _ => // Not a HikariDataSource
    }
  }
}