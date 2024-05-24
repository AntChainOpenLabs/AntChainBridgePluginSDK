package zaplogfmt

import (
	"errors"
	"fmt"
	"math"
	"strconv"
	"strings"
	"testing"
	"time"
	"unicode/utf8"

	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

func TestEncoderKey(t *testing.T) {
	tests := []struct {
		key      string
		expected string
	}{
		{`k`, `k=value`},
		{`k\`, `k\\=value`},
		{`k `, `k=value`},
		{`k=`, `k=value`},
		{`k"`, `k=value`},
		{`k` + string(utf8.RuneError), `k=value`},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}
			enc.AddString(tt.key, "value")
			assert.Equal(t, tt.expected, enc.buf.String())

			enc.AddString("x", "y")
			assert.Equal(t, tt.expected+" x=y", enc.buf.String())
		})
	}
}

func TestEncoderNamespaces(t *testing.T) {
	enc := &logfmtEncoder{
		EncoderConfig: &zapcore.EncoderConfig{},
		buf:           bufferpool.Get(),
	}

	enc.AddString("k", "value")
	for _, ns := range []string{"one", "two", "three"} {
		enc.OpenNamespace(ns)
		enc.AddString("k", "value")
	}
	assert.Equal(t, "k=value one.k=value one.two.k=value one.two.three.k=value", enc.buf.String())
}

func TestEncoderKeyNamespaceMap(t *testing.T) {
	tests := []struct {
		namespaces []string
		expected   string
	}{
		{[]string{`ns\`}, `ns\\.k=value`},
		{[]string{`ns `}, `ns.k=value`},
		{[]string{`ns=`}, `ns.k=value`},
		{[]string{`ns"`}, `ns.k=value`},
		{[]string{`ns` + string(utf8.RuneError)}, `ns.k=value`},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}
			for _, ns := range tt.namespaces {
				enc.OpenNamespace(ns)
			}
			enc.AddString("k", "value")
			assert.Equal(t, tt.expected, enc.buf.String())
		})
	}
}

func TestEncoderSimple(t *testing.T) {
	tests := []struct {
		desc     string
		expected string
		f        func(enc zapcore.Encoder)
	}{
		{"binary", "k=YmFzZTY0", func(enc zapcore.Encoder) { enc.AddBinary("k", []byte("base64")) }},
		{"bool (true)", "k=true", func(enc zapcore.Encoder) { enc.AddBool("k", true) }},
		{"bool (false)", "k=false", func(enc zapcore.Encoder) { enc.AddBool("k", false) }},
		{"bytestring", "k=bytes", func(enc zapcore.Encoder) { enc.AddByteString("k", []byte("bytes")) }},
		{"bytestring with nil", "k=", func(enc zapcore.Encoder) { enc.AddByteString("k", nil) }},
		{"bytestring with rune", `k=☺`, func(enc zapcore.Encoder) { enc.AddByteString("k", []byte{0xe2, 0x98, 0xba}) }},
		{"bytestring with decode error", `k="\ufffd"`, func(enc zapcore.Encoder) { enc.AddByteString("k", []byte{0xe2}) }},
		{"complex64", "k=1+2i", func(enc zapcore.Encoder) { enc.AddComplex64("k", 1+2i) }},
		{"complex128", "k=2+3i", func(enc zapcore.Encoder) { enc.AddComplex128("k", 2+3i) }},
		{"float32", "k=3.2", func(enc zapcore.Encoder) { enc.AddFloat32("k", 3.2) }},
		{"float32 +Inf", "k=+Inf", func(enc zapcore.Encoder) { enc.AddFloat32("k", float32(math.Inf(1))) }},
		{"float32 -Inf", "k=-Inf", func(enc zapcore.Encoder) { enc.AddFloat32("k", float32(math.Inf(-1))) }},
		{"float32 NaN", "k=NaN", func(enc zapcore.Encoder) { enc.AddFloat32("k", float32(math.NaN())) }},
		{"float64", "k=6.4", func(enc zapcore.Encoder) { enc.AddFloat64("k", 6.4) }},
		{"float64 +Inf", "k=+Inf", func(enc zapcore.Encoder) { enc.AddFloat64("k", math.Inf(1)) }},
		{"float64 -Inf", "k=-Inf", func(enc zapcore.Encoder) { enc.AddFloat64("k", math.Inf(-1)) }},
		{"float64 NaN", "k=NaN", func(enc zapcore.Encoder) { enc.AddFloat64("k", math.NaN()) }},
		{"int", "k=-1", func(enc zapcore.Encoder) { enc.AddInt("k", -1) }},
		{"int8", "k=-8", func(enc zapcore.Encoder) { enc.AddInt8("k", -8) }},
		{"int16", "k=-16", func(enc zapcore.Encoder) { enc.AddInt16("k", -16) }},
		{"int32", "k=-32", func(enc zapcore.Encoder) { enc.AddInt32("k", -32) }},
		{"int64", "k=-64", func(enc zapcore.Encoder) { enc.AddInt64("k", -64) }},
		{"string", "k=string", func(enc zapcore.Encoder) { enc.AddString("k", "string") }},
		{"string with spaces", `k="string with spaces"`, func(enc zapcore.Encoder) { enc.AddString("k", "string with spaces") }},
		{"string with quotes", `k="\"quoted string\""`, func(enc zapcore.Encoder) { enc.AddString("k", `"quoted string"`) }},
		{"string with backslash", `k=\\back\\`, func(enc zapcore.Encoder) { enc.AddString("k", `\back\`) }},
		{"string with newline", `k="new\nline"`, func(enc zapcore.Encoder) { enc.AddString("k", "new\nline") }},
		{"string with cr", `k="carriage\rreturn"`, func(enc zapcore.Encoder) { enc.AddString("k", "carriage\rreturn") }},
		{"string with tab", `k="tab\ttab"`, func(enc zapcore.Encoder) { enc.AddString("k", "tab\ttab") }},
		{"string with control char", `k="control\u0000char"`, func(enc zapcore.Encoder) { enc.AddString("k", "control\u0000char") }},
		{"string with rune", `k=☺`, func(enc zapcore.Encoder) { enc.AddString("k", "☺") }},
		{"string with decode error", `k="\ufffd"`, func(enc zapcore.Encoder) { enc.AddString("k", string([]byte{0xe2})) }},
		{"uint", "k=1", func(enc zapcore.Encoder) { enc.AddUint("k", 1) }},
		{"uint8", "k=8", func(enc zapcore.Encoder) { enc.AddUint8("k", 8) }},
		{"uint16", "k=16", func(enc zapcore.Encoder) { enc.AddUint16("k", 16) }},
		{"uint32", "k=32", func(enc zapcore.Encoder) { enc.AddUint32("k", 32) }},
		{"uint64", "k=64", func(enc zapcore.Encoder) { enc.AddUint64("k", 64) }},
		{"uintptr", "k=128", func(enc zapcore.Encoder) { enc.AddUintptr("k", 128) }},
		{"duration", "k=1", func(enc zapcore.Encoder) { enc.AddDuration("k", time.Nanosecond) }},
		{"time", "k=0", func(enc zapcore.Encoder) { enc.AddTime("k", time.Unix(0, 0)) }},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}

			tt.f(enc)
			assert.Equal(t, tt.expected, enc.buf.String(), fmt.Sprintf("desc: %s", tt.desc))

			enc.AddString("another", "field")
			assert.Equal(t, tt.expected+" another=field", enc.buf.String(), fmt.Sprintf("desc: %s with extra field", tt.desc))
		})
	}
}

type stringer string
type textMarshaler string

func (s stringer) String() string                    { return string(s) }
func (t textMarshaler) MarshalText() ([]byte, error) { return []byte(t), nil }

func TestEncoderReflected(t *testing.T) {
	dummyFunc := func(string) {}
	dummyCh := make(chan struct{})

	tests := []struct {
		desc     string
		expected string
		value    interface{}
	}{
		{"nil", "null", nil},
		{"error", "welp", errors.New("welp")},
		{"bytes", "bytes", []byte("bytes")},
		{"stringer", "my-stringer", stringer("my-stringer")},
		{"text marshaler", "marshaled-text", textMarshaler("marshaled-text")},
		{"bool", "true", true},
		{"int", "-1", -int(1)},
		{"int8", "-8", int8(-8)},
		{"int16", "-16", int8(-16)},
		{"int32", "-32", int8(-32)},
		{"int64", "-64", int8(-64)},
		{"uint", "1", uint(1)},
		{"uint8", "8", uint8(8)},
		{"uint16", "16", uint8(16)},
		{"uint32", "32", uint8(32)},
		{"uint64", "64", uint8(64)},
		{"float32", "3.2", float32(3.2)},
		{"float64", "6.4", float64(6.4)},
		{"string", "string", "string"},
		{"complex64", "1+2i", complex64(1 + 2i)},
		{"complex128", "2+3i", complex128(2 + 3i)},
		{"chan", fmt.Sprintf(`"%T(%p)"`, dummyCh, dummyCh), dummyCh},
		{"func", fmt.Sprintf("%T(%p)", dummyFunc, dummyFunc), dummyFunc},
		{"slice", "[0,1,2,3]", []int{0, 1, 2, 3}},
		{"map", `map[a:0]`, map[string]int{"a": 0}},
		{"array", "[one,two]", [2]string{"one", "two"}},
		{"ptr", "{}", &struct{}{}},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}

			err := enc.AddReflected("k", tt.value)
			assert.NoError(t, err)

			assert.Equal(t, "k="+tt.expected, enc.buf.String(), fmt.Sprintf("desc: %s", tt.desc))
		})
	}
}

type failedMarshaler string

func (t failedMarshaler) MarshalText() ([]byte, error) { return []byte(t), errors.New("welp") }

func TestEncoderReflectedFailed(t *testing.T) {
	enc := &logfmtEncoder{
		EncoderConfig: &zapcore.EncoderConfig{},
		buf:           bufferpool.Get(),
	}
	err := enc.AddReflected("k", failedMarshaler("marshaled"))
	assert.EqualError(t, err, "welp")
}

func TestArrayEncoderSimple(t *testing.T) {
	tests := []struct {
		desc     string
		expected string
		quoted   bool
		f        func(enc zapcore.ArrayEncoder)
	}{
		{"bool", "true", false, func(enc zapcore.ArrayEncoder) { enc.AppendBool(true) }},
		{"bytestring", "bytes", false, func(enc zapcore.ArrayEncoder) { enc.AppendByteString([]byte("bytes")) }},
		{"complex64", "1+2i", false, func(enc zapcore.ArrayEncoder) { enc.AppendComplex64(1 + 2i) }},
		{"complex128", "2+3i", false, func(enc zapcore.ArrayEncoder) { enc.AppendComplex128(2 + 3i) }},
		{"float32", "3.2", false, func(enc zapcore.ArrayEncoder) { enc.AppendFloat32(3.2) }},
		{"float64", "6.4", false, func(enc zapcore.ArrayEncoder) { enc.AppendFloat64(6.4) }},
		{"int", "-1", false, func(enc zapcore.ArrayEncoder) { enc.AppendInt(-1) }},
		{"int8", "-8", false, func(enc zapcore.ArrayEncoder) { enc.AppendInt8(-8) }},
		{"int16", "-16", false, func(enc zapcore.ArrayEncoder) { enc.AppendInt16(-16) }},
		{"int32", "-32", false, func(enc zapcore.ArrayEncoder) { enc.AppendInt32(-32) }},
		{"int64", "-64", false, func(enc zapcore.ArrayEncoder) { enc.AppendInt64(-64) }},
		{"string", "string-value", false, func(enc zapcore.ArrayEncoder) { enc.AppendString("string-value") }},
		{"uint", "1", false, func(enc zapcore.ArrayEncoder) { enc.AppendUint(1) }},
		{"uint8", "8", false, func(enc zapcore.ArrayEncoder) { enc.AppendUint8(8) }},
		{"uint16", "16", false, func(enc zapcore.ArrayEncoder) { enc.AppendUint16(16) }},
		{"uint32", "32", false, func(enc zapcore.ArrayEncoder) { enc.AppendUint32(32) }},
		{"uint64", "64", false, func(enc zapcore.ArrayEncoder) { enc.AppendUint64(64) }},
		{"uintptr", "128", false, func(enc zapcore.ArrayEncoder) { enc.AppendUintptr(128) }},
		{"duration", "1", false, func(enc zapcore.ArrayEncoder) { enc.AppendDuration(time.Nanosecond) }},
		{"time", "0", false, func(enc zapcore.ArrayEncoder) { enc.AppendTime(time.Unix(0, 0)) }},
		{"reflected", "{v}", false, func(enc zapcore.ArrayEncoder) { enc.AppendReflected(struct{ v string }{"v"}) }},
		{"reflected (quoted)", "{a b}", true, func(enc zapcore.ArrayEncoder) { enc.AppendReflected(struct{ a, b string }{"a", "b"}) }},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}

			enc.AddArray("x", zapcore.ArrayMarshalerFunc(func(enc zapcore.ArrayEncoder) error {
				tt.f(enc)
				return nil
			}))
			enc.AddArray("y", zapcore.ArrayMarshalerFunc(func(enc zapcore.ArrayEncoder) error {
				tt.f(enc)
				tt.f(enc)
				return nil
			}))

			expected := strings.Replace("x=[%%] y=[%%,%%]", "%%", tt.expected, -1)
			if tt.quoted {
				expected = strings.Replace(`x="[\"%%\"]" y="[\"%%\",\"%%\"]"`, "%%", tt.expected, -1)
			}
			assert.Equal(t, expected, enc.buf.String(), fmt.Sprintf("desc: %s", tt.desc))
		})
	}
}

func TestArrayEncoderError(t *testing.T) {
	enc := &logfmtEncoder{
		EncoderConfig: &zapcore.EncoderConfig{},
		buf:           bufferpool.Get(),
	}

	err := enc.AddArray("x", zapcore.ArrayMarshalerFunc(func(enc zapcore.ArrayEncoder) error {
		return errors.New("banana")
	}))
	assert.EqualError(t, err, "banana")
	assert.Equal(t, "x=", enc.buf.String())
}

func marshalIntArray(start, end int) zapcore.ArrayMarshalerFunc {
	return func(enc zapcore.ArrayEncoder) error {
		for i := start; i <= end; i++ {
			enc.AppendInt(i)
		}
		return nil
	}
}

func TestArrayEncoderComplex(t *testing.T) {
	tests := []struct {
		desc     string
		expected string
		f        zapcore.ArrayMarshalerFunc
	}{
		{
			desc:     "arrays in array",
			expected: "[0,[1,2,3],[4,5,6],7]",
			f: func(enc zapcore.ArrayEncoder) error {
				enc.AppendInt(0)
				enc.AppendArray(marshalIntArray(1, 3))
				enc.AppendArray(marshalIntArray(4, 6))
				enc.AppendInt(7)
				return nil
			},
		},
		{
			desc:     "array of objects",
			expected: `"[\"a=0\",\"b=1\",\"c=2\"]"`,
			f: func(enc zapcore.ArrayEncoder) error {
				for i := 0; i < 3; i++ {
					enc.AppendObject(zapcore.ObjectMarshalerFunc(func(oe zapcore.ObjectEncoder) error {
						oe.AddInt(string('a'+i), i)
						return nil
					}))
				}
				return nil
			},
		},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}

			err := enc.AddArray("x", tt.f)
			assert.NoError(t, err)

			assert.Equal(t, "x="+tt.expected, enc.buf.String())
		})
	}
}

func TestObjectEncoder(t *testing.T) {
	tests := []struct {
		desc     string
		expected string
		f        func(enc zapcore.ObjectEncoder)
	}{
		{"binary", "k=YmFzZTY0", func(enc zapcore.ObjectEncoder) { enc.AddBinary("k", []byte("base64")) }},
		{"bytestring", "k=bytes", func(enc zapcore.ObjectEncoder) { enc.AddByteString("k", []byte("bytes")) }},
		{"bool (true)", "k=true", func(enc zapcore.ObjectEncoder) { enc.AddBool("k", true) }},
		{"bool (false)", "k=false", func(enc zapcore.ObjectEncoder) { enc.AddBool("k", false) }},
		{"complex64", "k=1+2i", func(enc zapcore.ObjectEncoder) { enc.AddComplex64("k", 1+2i) }},
		{"complex128", "k=2+3i", func(enc zapcore.ObjectEncoder) { enc.AddComplex128("k", 2+3i) }},
		{"duration", "k=1", func(enc zapcore.ObjectEncoder) { enc.AddDuration("k", time.Nanosecond) }},
		{"float32", "k=3.2", func(enc zapcore.ObjectEncoder) { enc.AddFloat32("k", 3.2) }},
		{"float64", "k=6.4", func(enc zapcore.ObjectEncoder) { enc.AddFloat64("k", 6.4) }},
		{"int", "k=-1", func(enc zapcore.ObjectEncoder) { enc.AddInt("k", -1) }},
		{"int8", "k=-8", func(enc zapcore.ObjectEncoder) { enc.AddInt8("k", -8) }},
		{"int16", "k=-16", func(enc zapcore.ObjectEncoder) { enc.AddInt16("k", -16) }},
		{"int32", "k=-32", func(enc zapcore.ObjectEncoder) { enc.AddInt32("k", -32) }},
		{"int64", "k=-64", func(enc zapcore.ObjectEncoder) { enc.AddInt64("k", -64) }},
		{"string", "k=string", func(enc zapcore.ObjectEncoder) { enc.AddString("k", "string") }},
		{"time", "k=0", func(enc zapcore.ObjectEncoder) { enc.AddTime("k", time.Unix(0, 0)) }},
		{"uint", "k=1", func(enc zapcore.ObjectEncoder) { enc.AddUint("k", 1) }},
		{"uint8", "k=8", func(enc zapcore.ObjectEncoder) { enc.AddUint8("k", 8) }},
		{"uint16", "k=16", func(enc zapcore.ObjectEncoder) { enc.AddUint16("k", 16) }},
		{"uint32", "k=32", func(enc zapcore.ObjectEncoder) { enc.AddUint32("k", 32) }},
		{"uint64", "k=64", func(enc zapcore.ObjectEncoder) { enc.AddUint64("k", 64) }},
		{"uintptr", "k=128", func(enc zapcore.ObjectEncoder) { enc.AddUintptr("k", 128) }},
		{"reflected", "k={v}", func(enc zapcore.ObjectEncoder) { enc.AddReflected("k", struct{ v string }{"v"}) }},
		{"reflected (quoted)", `k=\"{a b}\"`, func(enc zapcore.ObjectEncoder) { enc.AddReflected("k", struct{ a, b string }{"a", "b"}) }},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}

			enc.AddObject("x", zapcore.ObjectMarshalerFunc(func(enc zapcore.ObjectEncoder) error {
				tt.f(enc)
				return nil
			}))

			expected := strings.Replace(`x="%%"`, "%%", tt.expected, -1)
			assert.Equal(t, expected, enc.buf.String(), fmt.Sprintf("desc: %s", tt.desc))
		})
	}
}

func TestObjectEncoderError(t *testing.T) {
	enc := &logfmtEncoder{
		EncoderConfig: &zapcore.EncoderConfig{},
		buf:           bufferpool.Get(),
	}

	err := enc.AddObject("x", zapcore.ObjectMarshalerFunc(func(enc zapcore.ObjectEncoder) error {
		return errors.New("mango-tango")
	}))
	assert.EqualError(t, err, "mango-tango")
	assert.Equal(t, "x=", enc.buf.String())
}

func TestObjectEncoderComplex(t *testing.T) {
	tests := []struct {
		desc     string
		expected string
		f        zapcore.ObjectMarshalerFunc
	}{
		{
			desc:     "objects with arrays",
			expected: `"a=[1,2,3] b=[4,5]"`,
			f: func(enc zapcore.ObjectEncoder) error {
				enc.AddArray("a", marshalIntArray(1, 3))
				enc.AddArray("b", marshalIntArray(4, 5))
				return nil
			},
		},
		{
			desc:     "objects with objects ",
			expected: `"0=\"a=1 b=2 c=3\" 1=\"d=4 e=5 f=6\" 2=\"g=7 h=8 i=9\""`,
			f: func(enc zapcore.ObjectEncoder) error {
				for i := 0; i < 3; i++ {
					enc.AddObject(strconv.Itoa(i), zapcore.ObjectMarshalerFunc(func(oe zapcore.ObjectEncoder) error {
						for j := 0; j < 3; j++ {
							oe.AddInt(string('a'+i*3+j), i*3+j+1)
						}
						return nil
					}))
				}
				return nil
			},
		},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{},
				buf:           bufferpool.Get(),
			}

			err := enc.AddObject("o", tt.f)
			assert.NoError(t, err)

			assert.Equal(t, "o="+tt.expected, enc.buf.String())
		})
	}
}

func TestEncodeTime(t *testing.T) {
	ts := time.Unix(1, 0)
	tests := []struct {
		desc        string
		expected    string
		timeEncoder zapcore.TimeEncoder
	}{
		{"nil", "1000000000", nil},
		{"epoch millis", "1000", zapcore.EpochMillisTimeEncoder},
		{"custom", "custom-time", func(t time.Time, enc zapcore.PrimitiveArrayEncoder) { enc.AppendString("custom-time") }},
		{"custom with spaces", `"with spaces"`, func(t time.Time, enc zapcore.PrimitiveArrayEncoder) { enc.AppendString("with spaces") }},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{EncodeTime: tt.timeEncoder},
				buf:           bufferpool.Get(),
			}
			enc.AddTime("ts", ts)
			assert.Equal(t, "ts="+tt.expected, enc.buf.String(), fmt.Sprintf("desc: %s", tt.desc))
		})
	}
}

func TestEncodeDuration(t *testing.T) {
	duration := time.Second
	tests := []struct {
		desc            string
		expected        string
		durationEncoder zapcore.DurationEncoder
	}{
		{"nil", "1000000000", nil},
		{"seconds", "1", zapcore.SecondsDurationEncoder},
		{"string", "1s", zapcore.StringDurationEncoder},
		{"custom", "custom", func(d time.Duration, enc zapcore.PrimitiveArrayEncoder) { enc.AppendString("custom") }},
		{"custom with spaces", `"with spaces"`, func(d time.Duration, enc zapcore.PrimitiveArrayEncoder) { enc.AppendString("with spaces") }},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := &logfmtEncoder{
				EncoderConfig: &zapcore.EncoderConfig{EncodeDuration: tt.durationEncoder},
				buf:           bufferpool.Get(),
			}
			enc.AddDuration("duration", duration)
			assert.Equal(t, "duration="+tt.expected, enc.buf.String(), fmt.Sprintf("desc: %s", tt.desc))
		})
	}
}

func TestClone(t *testing.T) {
	enc := &logfmtEncoder{
		EncoderConfig: &zapcore.EncoderConfig{},
		buf:           bufferpool.Get(),
	}
	enc.AddString("k", "v")

	clone := enc.Clone()
	assert.Equal(t, enc, clone, "clone should equal original encoder")

	enc.AddString("x", "y")
	assert.NotEqual(t, enc, clone, "clone should not equal original encoder")
}

func TestEncodeEntryConfig(t *testing.T) {
	tests := []struct {
		desc     string
		expected string
		ec       zapcore.EncoderConfig
		setup    func(enc zapcore.Encoder)
		fields   []zapcore.Field
	}{
		{
			desc:     "empty",
			expected: "",
		},
		{
			desc:     "empty with fields",
			expected: "key=value",
			fields:   []zapcore.Field{zap.String("key", "value")},
		},
		{
			desc:     "empty with context",
			expected: "message=message context=value field=value",
			ec:       zapcore.EncoderConfig{MessageKey: "message"},
			setup:    func(enc zapcore.Encoder) { enc.AddString("context", "value") },
			fields:   []zapcore.Field{zap.String("field", "value")},
		},
		{
			desc:     "TimeKey",
			expected: "time=1000000001",
			ec:       zapcore.EncoderConfig{TimeKey: "time"},
		},
		{
			desc:     "EncodeTime",
			expected: "ts=1.000000001",
			ec:       zapcore.EncoderConfig{TimeKey: "ts", EncodeTime: zapcore.EpochTimeEncoder},
		},
		{
			desc:     "LevelKey",
			expected: "level=debug",
			ec:       zapcore.EncoderConfig{LevelKey: "level"},
		},
		{
			desc:     "EncodeLevel",
			expected: "lvl=DEBUG",
			ec:       zapcore.EncoderConfig{LevelKey: "lvl", EncodeLevel: zapcore.CapitalLevelEncoder},
		},
		{
			desc:     "NameKey",
			expected: "name=test",
			ec:       zapcore.EncoderConfig{NameKey: "name"},
		},
		{
			desc:     "EncodeName",
			expected: "name=test",
			ec: zapcore.EncoderConfig{
				NameKey:    "name",
				EncodeName: zapcore.NameEncoder(func(s string, enc zapcore.PrimitiveArrayEncoder) {}),
			},
		},
		{
			desc:     "CallerKey",
			expected: "caller=arthur/philip/dent/h2g2.go:42",
			ec:       zapcore.EncoderConfig{CallerKey: "caller"},
		},
		{
			desc:     "EncodeCaller",
			expected: "caller=dent/h2g2.go:42",
			ec:       zapcore.EncoderConfig{CallerKey: "caller", EncodeCaller: zapcore.ShortCallerEncoder},
		},
		{
			desc:     "MessageKey",
			expected: "mesg=message",
			ec:       zapcore.EncoderConfig{MessageKey: "mesg"},
		},
		{
			desc:     "StracktraceKey",
			expected: `stack="stacktrace\nwith multiple lines\n\tand tabs\n"`,
			ec:       zapcore.EncoderConfig{StacktraceKey: "stack"},
		},
		{
			desc:     "LineEnding",
			expected: "",
			ec:       zapcore.EncoderConfig{LineEnding: "<EOL>"},
		},
	}

	for i, tt := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {
			enc := NewEncoder(tt.ec)
			if tt.setup != nil {
				tt.setup(enc)
			}

			entry := zapcore.Entry{
				Level:      zapcore.DebugLevel,
				Time:       time.Unix(1, 1),
				LoggerName: "test",
				Message:    "message",
				Caller: zapcore.EntryCaller{
					Defined: true,
					File:    "arthur/philip/dent/h2g2.go",
					Line:    42,
				},
				Stack: "stacktrace\nwith multiple lines\n\tand tabs\n",
			}

			buf, err := enc.EncodeEntry(entry, tt.fields)
			assert.NoError(t, err)

			lineEnding := tt.ec.LineEnding
			if lineEnding == "" {
				lineEnding = zapcore.DefaultLineEnding
			}
			assert.Equal(t, tt.expected+lineEnding, buf.String(), fmt.Sprintf("desc: %s", tt.desc))
		})
	}
}

func TestEncodeEntryEmptyConfigWithField(t *testing.T) {
	enc := NewEncoder(zapcore.EncoderConfig{})
	enc.AddString("x", "y")

	buf, err := enc.EncodeEntry(
		zapcore.Entry{},
		[]zapcore.Field{
			zap.String("a", "b"),
			zap.String("c", "d"),
		},
	)
	assert.NoError(t, err)
	assert.Equal(t, "x=y a=b c=d\n", buf.String())
}
