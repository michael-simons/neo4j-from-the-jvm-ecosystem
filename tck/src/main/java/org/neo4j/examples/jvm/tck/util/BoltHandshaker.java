/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.examples.jvm.tck.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

/**
 * This implements the protocol version negotiation of bolt. Testing to see if in address will respond to this is a
 * quick way to find out if it's a running bolt server.
 *
 * @author Andrew Jefferson
 * @author Michael J. Simons
 */
public final class BoltHandshaker {
	private static final int magicToken = 1616949271;

	// Versions message that cannot be matched because it is all zeros.
	private static final byte[] versionsMessage = {
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
	};

	private static boolean doBoltHandshake(String address, int port, int timeoutMillis) {
		try (Socket socket = new Socket()) {

			// Set the socket timeout for blocking operations
			socket.setSoTimeout(timeoutMillis);

			// Connects this socket to the server (also with the specified timeout value).
			socket.connect(new InetSocketAddress(address, port), timeoutMillis);

			DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
			DataInputStream dIn = new DataInputStream(socket.getInputStream());

			// Send magic token (0x6060B017)
			dOut.writeInt(magicToken);
			dOut.flush();

			// Send 4 supported versions
			// Except we don't support any versions and communicate that by sending all zeros
			dOut.write(versionsMessage);
			dOut.flush();

			// Receive agreed version
			// It should be 0 because there are no possible versions we can agree on
			int response = dIn.readInt();
			assert response == 0;

			// Because we cannot agree on a version the server should close its side of the connection
			// resulting in EOF (-1) on all subsequent reads.
			return dIn.read() == -1;
		} catch (IOException exception) {
			// Return false if handshake fails
			return false;
		}
	}

	public static void main(String... args) throws InterruptedException {

		var secondsToWait = Integer.parseInt(args[2]);
		var port = Integer.parseInt(args[1]);
		var waitingStartedAt = Instant.now();
		while (Duration.between(waitingStartedAt, Instant.now()).getSeconds() < secondsToWait) {
			if (doBoltHandshake(args[0], port, 500)) {
				System.exit(0);
			} else {
				Thread.sleep(500);
			}
		}
		throw new RuntimeException("Could not reach Neo4j in time");
	}
}