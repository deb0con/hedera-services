// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.4.9 <0.9.0;

interface IPrngSystemContract {
    // Generates a 256-bit pseudorandom seed using the first 256-bits of running hash of n-3 transaction record.
    // When n-3 running hash of transaction record is not present, doesn't return the 256-bit pseudorandom seed.
    function getPseudorandomSeed() external returns (bytes32);

    // Given an unsigned 32-bit integer "range", generates a pseudorandom number X within 0 <= X < range.
    // Uses the first 32-bits of running hash of n-3 transaction record to generate the pseudorandom number.
    // When running hash of n-3 transaction record is not present or invalid, doesn't return the pseudorandom number.
    // Need confirmation on use of seed ?
    function getPseudorandomNumber(uint32 range) external returns (uint32);
}