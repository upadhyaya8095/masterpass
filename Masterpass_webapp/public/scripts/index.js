$(document).ready(function () {
    console.log("ready!");
    const canMakePaymentButton = document.getElementById('canMakePaymentButton');
    const payButton = document.getElementById('buyButton');

    canMakePaymentButton.setAttribute('style', 'display: none;');
    payButton.setAttribute('style', 'display: none;');
    let displayScreen1 = function () {
        $("#modalserviceId,#responseContentId,#errresultId").hide();
        $("#mainContentId").show();
    }
    let displayScreen2 = function () {
        $("#mainContentId,#responseContentId,#errresultId").hide();
        $("#modalserviceId").show();
    }
    let closeResponse = function () {
        $("#responseContentId,#modalserviceId,#errresultId").hide();
        $("#mainContentId").show();
    }
    if (window.PaymentRequest) {
        payButton.setAttribute('style', 'display: inline;');
        canMakePaymentButton.setAttribute('style', 'display: inline;');

        canMakePaymentButton.addEventListener('click', function () {
            let request = initPaymentRequest();
            onCanMakePaymentButtonClicked(request);
        });
        /*********Invokes book button**********/
        payButton.addEventListener('click', function () {
            //$("#buyButton span").html("Loading..Please wait");
            //document.getElementById('result').style.display = "none";
            // let request = initPaymentRequest();
            //onBuyClicked(request);
            displayScreen2();
        });
    }

    $('#modalserviceId .modal-close').on("click", function () {
        displayScreen1();
    });
    $('#bookbtnId').on("click", function () {
        $("#bookbtnId .btnmainDiv span").html("Loading..Please wait");
        $("#errresultId").hide();
        //document.getElementById('result').style.display = "none";
        let request = initPaymentRequest();
			if (request.canMakePayment) {
			request.canMakePayment().then(function (result) {
            document.getElementById('canMakePaymentResult').innerHTML = result;
			if(result){
			onBuyClicked(request);
		}else{
			$("#bookbtnId .btnmainDiv span").html("Book");
			alert('No Payment App is installed');
		}
        }).catch(function (err) {
			$("#bookbtnId .btnmainDiv span").html("Book");
            console.log(err);
        });
    }
		/*if(onCanMakePaymentButtonClicked(request)){
			onBuyClicked(request);
		}else{
			alert('No Payment App is installed');
		}*/
        

        // $("#mainContentId,#modalserviceId").hide();
        // $("#responseContentId").show();
    });
    $('#closebtn,#errclosebtn').on("click", function () {
        closeResponse();
    });

});
if (location.protocol != 'https:')
    location.href = 'https:' + window.location.href.substring(window.location.protocol.length);
/********************details are based on checked value*****************************/
function initPaymentRequest() {
    var checkedValue = [];
    var inputElements = document.getElementsByClassName('messageCheckbox');
    for (var i = 0; inputElements[i]; ++i) {
        if (inputElements[i].checked) {
            checkedValue.push(inputElements[i].value);
        }
    }
    /**********Changes required for hardcoded values********/
    let pageTitle = document.title; // to get the dynamic value from title
    let supportedInstruments = [{
        supportedMethods: checkedValue,
        data: {
            merchantName: pageTitle,
            customKey: 'customValue'
        },
    }];

    let details = {
        total: {
            label: 'Total',
            amount: {
                currency: 'USD',
                value: '154.00'
            }
        },
        displayItems: [{
                label: 'Original amount',
                amount: {
                    currency: 'USD',
                    value: '164.00'
                },
            },
            {
                label: 'Friends and family discount',
                amount: {
                    currency: 'USD',
                    value: '-10.00'
                },
            },
        ],
    };

    return new PaymentRequest(supportedInstruments, details);
}

/**
 * Invokes PaymentRequest.
 *
 * @param {PaymentRequest} request The PaymentRequest object.
 */
function onBuyClicked(request) {
    $("#bookbtnId .btnmainDiv span").html("Book");
    request.show().then(function (instrumentResponse) {
            instrumentResponse.complete('success')
                .then(function () {
                    //document.getElementById('result').innerHTML =
                    //instrumentToJsonString(instrumentResponse);
                    let responseObj = instrumentResponse.details.networkTokenizedCardResponse;
                    let status = responseObj.status;
                    if(status!='fail'){
                        $("#mainContentId,#modalserviceId").hide();
                        $("#responseContentId").show();
                    }else{
                        $("#mainContentId,#modalserviceId").hide();
                        $("#errresultId").show();
                        //document.getElementById('#errresultId').style.display = "block";
                    }
                    //document.getElementById('result').style.display = "block";
                   
                    window.scrollTo(0, document.body.scrollHeight);
                }).catch(function (err) {
                    $("#errresultId").hide();
                    console.log(err);
                });
        })
        .catch(function (err) {
            console.log(err);
        });
}

/**
 * Invokes canMakePayment
 *
 * @param {PaymentRequest} request The PaymentRequest object.
 */
function onCanMakePaymentButtonClicked(request) {
    document.getElementById('canMakePaymentResult').innerHTML = "";
    if (request.canMakePayment) {
        request.canMakePayment().then(function (result) {
            document.getElementById('canMakePaymentResult').innerHTML = result;
			return result;
        }).catch(function (err) {
            console.log(err);
        });
    }
}

function instrumentToJsonString(instrument) {
    if (instrument.toJSON) {
        return JSON.stringify(instrument, undefined, 2);
    } else {
        return JSON.stringify({
            methodName: instrument.methodName,
            details: instrument.details,
        }, undefined, 2);
    }
}