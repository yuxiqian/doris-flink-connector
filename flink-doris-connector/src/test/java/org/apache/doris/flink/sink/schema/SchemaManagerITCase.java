// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.flink.sink.schema;

import org.apache.doris.flink.DorisTestBase;
import org.apache.doris.flink.catalog.doris.FieldSchema;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.exception.IllegalArgumentException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaManagerITCase extends DorisTestBase {

    private static final String DATABASE = "test_sc_db";
    private DorisOptions options;
    private SchemaChangeManager schemaChangeManager;

    @Before
    public void setUp() throws Exception {
        options =
                DorisOptions.builder()
                        .setFenodes(getFenodes())
                        .setTableIdentifier(DATABASE + ".add_column")
                        .setUsername(USERNAME)
                        .setPassword(PASSWORD)
                        .build();
        schemaChangeManager = new SchemaChangeManager(options);
    }

    private void initDorisSchemaChangeTable(String table) throws SQLException {
        try (Connection connection =
                        DriverManager.getConnection(
                                String.format(URL, DORIS_CONTAINER.getHost()), USERNAME, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute(String.format("CREATE DATABASE IF NOT EXISTS %s", DATABASE));
            statement.execute(String.format("DROP TABLE IF EXISTS %s.%s", DATABASE, table));
            statement.execute(
                    String.format(
                            "CREATE TABLE %s.%s ( \n"
                                    + "`id` varchar(32),\n"
                                    + "`age` int\n"
                                    + ") DISTRIBUTED BY HASH(`id`) BUCKETS 1\n"
                                    + "PROPERTIES (\n"
                                    + "\"replication_num\" = \"1\"\n"
                                    + ")\n",
                            DATABASE, table));
        }
    }

    @Test
    public void testAddColumn() throws SQLException, IOException, IllegalArgumentException {
        String addColumnTbls = "add_column";
        initDorisSchemaChangeTable(addColumnTbls);
        FieldSchema field = new FieldSchema("c1", "int", "");
        schemaChangeManager.addColumn(DATABASE, addColumnTbls, field);
        boolean exists = schemaChangeManager.addColumn(DATABASE, addColumnTbls, field);
        Assert.assertTrue(exists);

        exists = schemaChangeManager.checkColumnExists(DATABASE, addColumnTbls, "c1");
        Assert.assertTrue(exists);
    }

    @Test
    public void testDropColumn() throws SQLException, IOException, IllegalArgumentException {
        String dropColumnTbls = "drop_column";
        initDorisSchemaChangeTable(dropColumnTbls);
        schemaChangeManager.dropColumn(DATABASE, dropColumnTbls, "age");
        boolean success = schemaChangeManager.dropColumn(DATABASE, dropColumnTbls, "age");
        Assert.assertTrue(success);

        boolean exists = schemaChangeManager.checkColumnExists(DATABASE, dropColumnTbls, "age");
        Assert.assertFalse(exists);
    }

    @Test
    public void testRenameColumn() throws SQLException, IOException, IllegalArgumentException {
        String renameColumnTbls = "rename_column";
        initDorisSchemaChangeTable(renameColumnTbls);
        schemaChangeManager.renameColumn(DATABASE, renameColumnTbls, "age", "age1");
        boolean exists = schemaChangeManager.checkColumnExists(DATABASE, renameColumnTbls, "age1");
        Assert.assertTrue(exists);

        exists = schemaChangeManager.checkColumnExists(DATABASE, renameColumnTbls, "age");
        Assert.assertFalse(exists);
    }
}
