package org.scalarelational.extra

import javax.sql.DataSource

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalarelational.model.SQLDatastore

trait HikariSupport extends SQLDatastore {
  // Automatically converts DataSources to be wrapped by HikariDataSource
  dataSourceProperty.filterCycles.attach {
    case Some(ds: HikariDataSource) => // Ignore HikariDataSource
    case Some(ds) => {
      val config = new HikariConfig()
      config.setDataSource(ds)

      val hikari = new HikariDataSource(config)
      dataSourceProperty := hikari
    }
    case None => // Ignore no datasource being defined
  }

  override protected def disposeDataSource(dataSource: DataSource): Unit = {
    super.disposeDataSource(dataSource)

    dataSource match {
      case ds: HikariDataSource => {
        Option(ds.getDataSource).foreach(disposeDataSource)
        ds.close()
      }
      case _ => // Ignore
    }
  }
}