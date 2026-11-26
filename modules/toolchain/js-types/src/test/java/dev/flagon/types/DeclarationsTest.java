package dev.flagon.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeclarationsTest {

    @Test
    void declaresExportsAndTheTypesTheyName() {
        String expected = """
                // generated from dev.flagon.types.fixture.FixtureMain
                export function main(args: string[]): void;
                export function add(a: number, b: number): number;
                export function greet(in_: string, at: Point): string;
                export function sum(...values: number[]): bigint;
                export function samples(data: Int8Array, labels: string[]): Float32Array;
                export function each(visitor: (point: Point, weight: number) => boolean): void;
                declare function delete_(): void;
                export { delete_ as delete };

                export class Counter {
                    constructor(start: number);
                    readonly value: number;
                    static zero(): Counter;
                    increment(): number;
                }

                export interface Point extends Shape {
                    x: number;
                    readonly visible: boolean;
                    toString(): string;
                    label(prefix: string, extra: unknown): string;
                }

                export interface Shape {
                    readonly kind: string;
                }
                """;
        assertEquals(expected, new Declarations(getClass().getClassLoader())
                .module("dev.flagon.types.fixture.FixtureMain"));
    }
}
