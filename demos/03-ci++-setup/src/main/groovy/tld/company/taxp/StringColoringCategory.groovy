package tld.company.taxp

@Category(String)
class StringColoringCategory {

    String error() {
        "\033[1;33m${this}\033[0m"
    }

    String warn() {
        "\033[1;33m${this}\033[0m"
    }

    String info() {
        "\033[1;32m${this}\033[0m"
    }

    String note() {
        "\033[1;36m${this}\033[0m"
    }
}
